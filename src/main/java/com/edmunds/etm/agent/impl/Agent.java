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

import com.edmunds.etm.common.api.AgentPaths;
import com.edmunds.zookeeper.connection.ZooKeeperConnection;
import com.edmunds.zookeeper.connection.ZooKeeperNodeInitializer;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * An Agent allows ETM to manage a web server.
 * <p/>
 * Each agent is tied to a single web server. It is responsible for deploying ETM-generated configuration data
 * (consisting primarily of URL to VIP mapping rules) and monitoring the server.
 *
 * @author Ryan Holmes
 */
@Component
public class Agent implements Runnable, InitializingBean, DisposableBean {
    private static final Logger logger = Logger.getLogger(Agent.class);

    private final ZooKeeperConnection connection;
    private final RuleSetMonitor ruleSetMonitor;
    private final AgentReporter agentReporter;
    private final AgentPaths agentPaths;

    @Autowired
    public Agent(ZooKeeperConnection connection,
                 RuleSetMonitor ruleSetMonitor,
                 AgentReporter agentReporter,
                 AgentPaths agentPaths) {
        this.connection = connection;
        this.ruleSetMonitor = ruleSetMonitor;
        this.agentReporter = agentReporter;
        this.agentPaths = agentPaths;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        connection.addInitializer(new ZooKeeperNodeInitializer(agentPaths.getStructuralPaths()));
        connection.addListener(ruleSetMonitor);
        connection.addListener(agentReporter);
    }

    @Override
    public void run() {
        logger.info("*** Starting ETM Agent ***");
        connection.connect();

        try {
            synchronized (this) {
                wait();
            }
        } catch (InterruptedException e) {
            logger.error("Main thread interrupted, exiting", e);
        }
    }

    @Override
    public void destroy() throws Exception {
        logger.info("*** Stopping ETM Agent ***");
        if (connection != null) {
            connection.close();
        }
    }
}
