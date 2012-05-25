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

import com.edmunds.etm.agent.api.WebServerController;
import com.edmunds.etm.common.api.AgentPaths;
import com.edmunds.etm.common.api.ControllerPaths;
import com.edmunds.zookeeper.connection.ZooKeeperConnection;
import com.edmunds.zookeeper.connection.ZooKeeperConnectionListener;
import com.edmunds.zookeeper.connection.ZooKeeperConnectionState;
import org.apache.log4j.Logger;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * Monitors the Apache rule set node.
 *
 * @author Ryan Holmes
 */
@Component
public class RuleSetMonitor implements ZooKeeperConnectionListener, Watcher {

    private static final Logger logger = Logger.getLogger(RuleSetMonitor.class);

    private final ZooKeeperConnection connection;
    private final WebServerController serverController;
    private final AgentReporter agentReporter;
    private final String ruleSetNodePath;
    private final AgentPaths agentPaths;
    private final RuleSetDeploymentExecutor deploymentExecutor;

    @Autowired
    public RuleSetMonitor(ZooKeeperConnection connection,
                          WebServerController serverController,
                          AgentReporter agentReporter,
                          ControllerPaths controllerPaths,
                          AgentPaths agentPaths) {

        this.connection = connection;
        this.serverController = serverController;
        this.agentReporter = agentReporter;
        this.ruleSetNodePath = controllerPaths.getApacheConf();
        this.agentPaths = agentPaths;
        this.deploymentExecutor = new RuleSetDeploymentExecutor();
    }

    @Override
    public void onConnectionStateChanged(ZooKeeperConnectionState state) {
        if (state == ZooKeeperConnectionState.INITIALIZED) {

            AsyncCallback.StatCallback cb = new AsyncCallback.StatCallback() {
                @Override
                public void processResult(int rc, String path, Object ctx, Stat stat) {
                    onRuleSetNodeExists(Code.get(rc), path);
                }
            };
            connection.exists(ruleSetNodePath, this, cb, null);
        }
    }

    @Override
    public void process(WatchedEvent event) {
        String path = event.getPath();
        if (path != null && path.equals(ruleSetNodePath)) {
            switch (event.getType()) {
                case NodeCreated:
                case NodeDataChanged:
                    AsyncCallback.DataCallback cb = new AsyncCallback.DataCallback() {
                        @Override
                        public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
                            onGetRuleSetData(Code.get(rc), path, data);
                        }
                    };
                    connection.getData(ruleSetNodePath, this, cb, null);
                case None:
                case NodeDeleted:
                case NodeChildrenChanged:
                default:
                    // Ignore other event types
            }
        }
    }

    /**
     * Handles configuration node existence check.
     *
     * @param rc   result code
     * @param path node path
     */
    protected void onRuleSetNodeExists(Code rc, String path) {

        final AsyncCallback.DataCallback cb = new AsyncCallback.DataCallback() {
            @Override
            public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
                onGetRuleSetData(Code.get(rc), path, data);
            }
        };

        if (rc == Code.OK) {
            connection.getData(ruleSetNodePath, this, cb, null);
        } else if (rc == Code.NONODE) {
            logger.info(String.format("Rule set node %s does not exist", path));
        } else {
            logger.error(String.format("Error %s while checking for rule set node %s", rc, path));
        }
    }

    /**
     * Handles rule set data.
     * <p/>
     *
     * @param rc   result code
     * @param path node path
     * @param data rule set data
     */
    protected void onGetRuleSetData(Code rc, String path, byte[] data) {

        logger.debug("Received rule set data");

        if (rc != Code.OK) {
            logger.error(String.format("Error %s while getting rule set data for node %s", rc, path));
            return;
        }

        // Ensure that configuration data is non-null
        byte[] ruleSetData = data == null ? new byte[0] : data;

        RuleSetDeploymentTask task = new RuleSetDeploymentTask(
                ruleSetData,
                serverController,
                connection,
                agentReporter,
                agentPaths);

        deploymentExecutor.execute(task);
    }

    /**
     * Coordinates the deployment of web server rules.
     * <p/>
     * This class ensures that deployment tasks run sequentially
     */
    private class RuleSetDeploymentExecutor implements Executor {
        Runnable pending;
        Runnable active;

        public synchronized void execute(final Runnable r) {
            logger.debug("Queueing new rule set deployment task");
            pending = new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } finally {
                        scheduleNext();
                    }
                }
            };

            if (active == null) {
                scheduleNext();
            }
        }

        protected synchronized void scheduleNext() {
            active = pending;
            if (active != null) {
                pending = null;
                new Thread(active).start();
            }
        }
    }
}
