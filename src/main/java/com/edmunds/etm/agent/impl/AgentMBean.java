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

import com.edmunds.etm.common.api.AgentInstance;
import com.edmunds.etm.common.api.RuleSetDeploymentEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Agent monitoring MBean.
 *
 * @author Ryan Holmes
 */
@Component
@ManagedResource(objectName = "Edmunds:type=ETM,name=Agent", description = "ETM Agent")
public class AgentMBean {

    private final AgentReporter agentReporter;

    @Autowired
    public AgentMBean(AgentReporter agentReporter) {
        this.agentReporter = agentReporter;
    }

    @ManagedAttribute(description = "Agent IP address")
    public String getIpAddress() {
        return getAgentInstance().getIpAddress();
    }

    @ManagedAttribute(description = "Agent version")
    public String getVersion() {
        return getAgentInstance().getVersion();
    }

    @ManagedAttribute(description = "Active rule set digest")
    public String getActiveRuleSetDigest() {
        return getAgentInstance().getActiveRuleSetDigest();
    }

    @ManagedAttribute(description = "Date of last rule set deployment")
    public Date getLastDeploymentDate() {
        return getLastDeployment() != null ? getLastDeployment().getEventDate() : null;
    }

    @ManagedAttribute(description = "Digest of last rule set deployment")
    public String getLastDeploymentRuleSetDigest() {
        return getLastDeployment() != null ? getLastDeployment().getRuleSetDigest() : "";
    }

    @ManagedAttribute(description = "Result of last rule set deployment")
    public String getLastDeploymentResult() {
        return getLastDeployment() != null ? getLastDeployment().getResult().toString() : "";
    }

    @ManagedAttribute(description = "Date of last failed rule set deployment")
    public Date getLastFailedDeploymentDate() {
        return getLastFailedDeployment() != null ? getLastFailedDeployment().getEventDate() : null;
    }

    @ManagedAttribute(description = "Digest of last failed rule set deployment")
    public String getLastFailedDeploymentRuleSetDigest() {
        return getLastFailedDeployment() != null ? getLastFailedDeployment().getRuleSetDigest() : "";
    }

    @ManagedAttribute(description = "Result of last failed rule set deployment")
    public String getLastFailedDeploymentResult() {
        return getLastFailedDeployment() != null ? getLastFailedDeployment().getResult().toString() : "";
    }

    private AgentInstance getAgentInstance() {
        return agentReporter.getAgentInstance();
    }

    private RuleSetDeploymentEvent getLastDeployment() {
        return getAgentInstance().getLastDeploymentEvent();
    }

    private RuleSetDeploymentEvent getLastFailedDeployment() {
        return getAgentInstance().getLastFailedDeploymentEvent();
    }
}
