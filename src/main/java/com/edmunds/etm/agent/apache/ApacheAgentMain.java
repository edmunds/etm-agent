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
package com.edmunds.etm.agent.apache;

import com.edmunds.etm.agent.impl.Agent;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * The Apache agent application.
 *
 * This class provides a main method to start an Apache agent.
 *
 * @author Ryan Holmes
 */
public final class ApacheAgentMain {

    public static void main(String[] args) {

        // Create the Spring application context
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("etm-agent-context.xml");
        ctx.registerShutdownHook();

        // Run the Apache agent
        Agent agent;
        agent = (Agent) ctx.getBean("agent", Agent.class);
        agent.run();
    }

    private ApacheAgentMain() {
        // This class should never be instantiated.
    }
}
