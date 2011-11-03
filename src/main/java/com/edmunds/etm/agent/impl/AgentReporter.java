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
import com.edmunds.etm.agent.api.WebServerController;
import com.edmunds.etm.common.api.AgentInstance;
import com.edmunds.etm.common.api.AgentPaths;
import com.edmunds.etm.common.api.RuleSetDeploymentEvent;
import com.edmunds.etm.common.api.RuleSetDeploymentResult;
import com.edmunds.etm.common.impl.ObjectSerializer;
import com.edmunds.etm.common.thrift.AgentInstanceDto;
import com.edmunds.zookeeper.connection.ZooKeeperConnection;
import com.edmunds.zookeeper.connection.ZooKeeperConnectionListener;
import com.edmunds.zookeeper.connection.ZooKeeperConnectionState;
import com.edmunds.zookeeper.util.ZooKeeperUtils;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Reports agent status and events to ZooKeeper for central monitoring.
 *
 * @author Ryan Holmes
 */
@Component
public class AgentReporter implements ZooKeeperConnectionListener {

    private static final Logger logger = Logger.getLogger(AgentReporter.class);

    private final ZooKeeperConnection connection;
    private final AgentPaths agentPaths;
    private final WebServerController webServerController;
    private final ObjectSerializer objectSerializer;
    private final ProjectProperties projectProperties;

    private AgentInstance agentInstance;
    private String agentNodePath;

    @Autowired
    public AgentReporter(ZooKeeperConnection connection,
                         AgentPaths agentPaths,
                         WebServerController webServerController,
                         ObjectSerializer objectSerializer,
                         ProjectProperties projectProperties) {
        this.connection = connection;
        this.agentPaths = agentPaths;
        this.webServerController = webServerController;
        this.objectSerializer = objectSerializer;
        this.projectProperties = projectProperties;
    }

    /**
     * Publishes a new rule set deployment event.
     *
     * @param event rule set deployment event
     * @param activeRuleSetDigest digest of the active rule set
     */
    public void publishDeploymentEvent(RuleSetDeploymentEvent event, String activeRuleSetDigest) {
        agentInstance.setLastDeploymentEvent(event);
        if(event.getResult() != RuleSetDeploymentResult.OK) {
            agentInstance.setLastFailedDeploymentEvent(event);
        }
        agentInstance.setActiveRuleSetDigest(activeRuleSetDigest);

        updateAgentNode();
    }

    /**
     * Returns the agent instance object. <p/> The agent instance is created lazily as needed, so this method will never
     * return null.
     *
     * @return agent instance object
     */
    public AgentInstance getAgentInstance() {
        if(agentInstance == null) {
            agentInstance = createAgentInstance();
        }
        return agentInstance;
    }

    @Override
    public void onConnectionStateChanged(ZooKeeperConnectionState state) {
        if(state == ZooKeeperConnectionState.INITIALIZED) {
            createAgentNode();
        } else if(state == ZooKeeperConnectionState.EXPIRED) {
            agentNodePath = null;
        }
    }

    /**
     * Creates an ephemeral node to represent this agent instance.
     */
    protected void createAgentNode() {

        if(agentNodePath != null) {
            return;
        }

        agentInstance = createAgentInstance();

        agentNodePath = agentPaths.getConnectedHost(agentInstance.getIpAddress()) + "-";
        byte[] data = agentInstanceToBytes(agentInstance);

        AsyncCallback.StringCallback callback = new AsyncCallback.StringCallback() {
            @Override
            public void processResult(int rc, String path, Object ctx, String name) {
                onAgentNodeCreated(Code.get(rc), path, name);
            }
        };

        connection.createEphemeralSequential(agentNodePath, data, callback, null);
    }

    protected void onAgentNodeCreated(Code rc, String path, String name) {
        if(rc == Code.OK) {
            agentNodePath = name;
            logger.debug(String.format("Created agent host node: %s", name));
        } else if(ZooKeeperUtils.isRetryableError(rc)) {
            logger.warn(String.format("Error %s while creating agent instance node %s, retrying", rc, path));
            createAgentNode();
        } else {
            // Unrecoverable error
            logger.error(String.format("Error %s while creating agent instance node: %s", rc, path));
        }
    }

    protected void updateAgentNode() {
        if(agentNodePath == null) {
            return;
        }

        byte[] data = agentInstanceToBytes(agentInstance);

        AsyncCallback.StatCallback cb = new AsyncCallback.StatCallback() {
            @Override
            public void processResult(int rc, String path, Object ctx, Stat stat) {
                onSetAgentNodeData(Code.get(rc), path);
            }
        };

        connection.setData(agentNodePath, data, -1, cb, null);
    }

    protected void onSetAgentNodeData(Code rc, String path) {

        if(rc == Code.OK) {
            logger.debug("Agent instance node updated");
        } else if(ZooKeeperUtils.isRetryableError(rc)) {
            logger.warn(String.format("Error %s while updating agent instance node %s, retrying", rc, path));
            updateAgentNode();
        } else {
            // Unrecoverable error
            logger.error(String.format("Error %s while updating agent instance node: %s", rc, path));
        }
    }

    private AgentInstance createAgentInstance() {
        UUID agentId = UUID.randomUUID();
        String ipAddress = getIpAddress();
        String version = getAgentVersion();

        AgentInstance instance = new AgentInstance(agentId, ipAddress, version);

        // Set the active rule set digest
        byte[] ruleSetData = webServerController.readRuleSetData();
        instance.setActiveRuleSetDigest(AgentUtils.ruleSetDigest(ruleSetData));

        return instance;
    }

    private static String getIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch(UnknownHostException e) {
            String message = "Could not get IP address for localhost";
            logger.error(message, e);
            throw new RuntimeException(e);
        }
    }

    private byte[] agentInstanceToBytes(AgentInstance instance) {
        AgentInstanceDto dto = AgentInstance.writeDto(instance);
        byte[] data = new byte[0];
        try {
            data = objectSerializer.writeValue(dto);
        } catch(IOException e) {
            logger.error(String.format("Failed to serialize controller instance dto: %s", dto), e);
        }

        return data;
    }

    private String getAgentVersion() {
        return projectProperties.getVersion();
    }
}
