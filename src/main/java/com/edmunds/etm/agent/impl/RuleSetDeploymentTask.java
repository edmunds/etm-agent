/*
 * Copyright 2011 Edmunds.com, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.edmunds.etm.agent.impl;

import com.edmunds.etm.agent.AgentUtils;
import com.edmunds.etm.agent.api.HealthCheckListener;
import com.edmunds.etm.agent.api.WebServerController;
import com.edmunds.etm.common.api.AgentPaths;
import com.edmunds.etm.common.api.RuleSetDeploymentEvent;
import com.edmunds.etm.common.api.RuleSetDeploymentResult;
import com.edmunds.zookeeper.connection.ZooKeeperConnection;
import com.edmunds.zookeeper.election.ZooKeeperElection;
import com.edmunds.zookeeper.election.ZooKeeperElectionListener;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Date;

/**
 * Deploys new rule set data to the web server.
 *
 * @author Ryan Holmes
 */
public class RuleSetDeploymentTask implements Runnable, ZooKeeperElectionListener, HealthCheckListener {

    private static final Logger logger = Logger.getLogger(RuleSetDeploymentTask.class);

    private final byte[] newRuleSetData;
    private final WebServerController serverController;
    private final ZooKeeperConnection connection;
    private final AgentReporter agentReporter;
    private final ZooKeeperElection restartElection;

    private final Integer lock = -1;

    private byte[] oldRuleSetData;
    private RuleSetDeploymentResult deploymentResult;
    private boolean ruleSetRolledBack;
    private String newRuleSetDigest;
    private String oldRuleSetDigest;

    public RuleSetDeploymentTask(byte[] newRuleSetData,
                                 WebServerController serverController,
                                 ZooKeeperConnection connection,
                                 AgentReporter agentReporter,
                                 AgentPaths agentPaths) {
        this.newRuleSetData = newRuleSetData;
        this.serverController = serverController;
        this.connection = connection;
        this.agentReporter = agentReporter;
        this.restartElection = new ZooKeeperElection(connection, agentPaths.getRestartElection());
        this.deploymentResult = RuleSetDeploymentResult.UNKNOWN;

        this.restartElection.addListener(this);
    }

    /**
     * This method stores the existing web server configuration to {@link #oldRuleSetData} and compares it to the new
     * configuration data. If there are any differences, the restart election process is initiated and it controls the
     * actual configuration update and restart of the Apache server. Otherwise, no changes are made.
     */
    @Override
    public void run() {

        logger.info(String.format("Deploying rule set %s", getNewRuleSetDigest()));

        // Get the current configuration
        oldRuleSetData = serverController.readRuleSetData();

        if(Arrays.equals(oldRuleSetData, newRuleSetData)) {
            // No change to rule set data, report as successful deployment
            logger.info(String.format("Current rule set %s is up to date", getOldRuleSetDigest()));
            deploymentResult = RuleSetDeploymentResult.OK;
            reportDeploymentEvent();
            return;
        }

        restartElection.enroll();

        logger.info("Waiting for leadership election");
        try {
            synchronized(lock) {
                lock.wait();
            }
        } catch(InterruptedException e) {
            logger.error("Rule set deployment task interrupted", e);
            connection.reconnect();
        }
    }

    @Override
    public void onElectionStateChange(ZooKeeperElection zooKeeperElection, boolean master) {
        if (master) {
            deployNewRuleSet();
        } else {
            reportDeploymentEvent();
            exit();
        }
    }

    @Override
    public void onHealthCheckComplete(boolean alive) {
        if(alive) {
            if(!ruleSetRolledBack) {
                deploymentResult = RuleSetDeploymentResult.OK;
            }
            restartElection.withdraw();
        } else if(ruleSetRolledBack) {
            logger.error(String.format("Rollback failed with rule set %s", getNewRuleSetDigest()));
            deploymentResult = RuleSetDeploymentResult.ROLLBACK_FAILED;
            restartElection.withdraw();
        } else {
            logger.error(String.format("Health check failed with rule set %s", getNewRuleSetDigest()));
            deploymentResult = RuleSetDeploymentResult.HEALTH_CHECK_FAILED;
            rollBackRuleSet();
        }
    }

    /**
     * Deploys the new rule set data and restarts the web server.
     * <p/>
     * This method checks the syntax of the new rule set data and rolls back to the current rule set if an error is
     * detected. It will also roll back if the server restart command or the health check fails.
     */
    private void deployNewRuleSet() {


        // Write the new rule set file
        logger.info(String.format("Writing data for rule set %s", getNewRuleSetDigest()));
        serverController.writeRuleSetData(newRuleSetData);

        // Test the syntax
        boolean syntaxOk = serverController.checkSyntax();

        if(!syntaxOk) {
            logger.error(String.format("Syntax check failed with rule set %s", getNewRuleSetDigest()));
            deploymentResult = RuleSetDeploymentResult.SYNTAX_CHECK_FAILED;

            // Restore old rule set
            rollBackRuleSet();
            return;
        }

        // Restart with new rule set
        logger.info(String.format("Restarting server with rule set %s", getNewRuleSetDigest()));
        boolean restartOk = serverController.restart();
        if(!restartOk) {
            logger.warn(String.format("Server restart failed with rule set %s", getNewRuleSetDigest()));
            deploymentResult = RuleSetDeploymentResult.RESTART_COMMAND_FAILED;

            // Restore old rule set
            rollBackRuleSet();
            return;
        }

        // Syntax check and restart ok, execute health check
        serverController.newHealthCheck().execute(this);
    }

    /**
     * Rolls back the rule set back to the original data.
     */
    private void rollBackRuleSet() {
        logger.info(String.format("Rolling back to rule set %s", getOldRuleSetDigest()));
        serverController.writeRuleSetData(oldRuleSetData);
        ruleSetRolledBack = true;

        boolean success = serverController.restart();
        if(!success) {
            deploymentResult = RuleSetDeploymentResult.RESTART_COMMAND_FAILED;
            logger.error(String.format("Server restart failed with rule set %s", getOldRuleSetDigest()));
        }
        serverController.newHealthCheck().execute(this);
    }

    private void reportDeploymentEvent() {
        Date eventDate = new Date();
        String ruleSetDigest = getNewRuleSetDigest();
        RuleSetDeploymentEvent event = new RuleSetDeploymentEvent(eventDate, ruleSetDigest, deploymentResult);

        String activeRuleSetDigest;
        if(deploymentResult == RuleSetDeploymentResult.OK) {
            activeRuleSetDigest = ruleSetDigest;
        } else {
            activeRuleSetDigest = getOldRuleSetDigest();
        }
        agentReporter.publishDeploymentEvent(event, activeRuleSetDigest);
    }

    private String getNewRuleSetDigest() {
        if(newRuleSetDigest == null) {
            newRuleSetDigest = AgentUtils.ruleSetDigest(newRuleSetData);
        }
        return newRuleSetDigest;
    }

    private String getOldRuleSetDigest() {
        if(oldRuleSetDigest == null) {
            oldRuleSetDigest = AgentUtils.ruleSetDigest(oldRuleSetData);
        }
        return oldRuleSetDigest;
    }

    private void exit() {
        synchronized(lock) {
            lock.notifyAll();
        }
    }
}
