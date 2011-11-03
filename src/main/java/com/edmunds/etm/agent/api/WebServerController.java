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
package com.edmunds.etm.agent.api;

/**
 * Interface for a web server controller. A WebServerController provides methods to manipulate a server's ETM rule set
 * data (e.g. an included Apache configuration file), to check rule set syntax and to restart the server.
 *
 * @author Ryan Holmes
 */
public interface WebServerController {

    /**
     * Reads the current ETM rule set data from the server.
     * <p/>
     * An empty byte array is returned if no rule set data exists.
     *
     * @return rule set data or an empty byte array, never {@code null}
     */
    public byte[] readRuleSetData();

    /**
     * Writes the specified rule set data to the server.
     * <p/>
     * Note: Implementations must handle null ruleSetData.
     *
     * @param ruleSetData the rule set data to write
     */
    public void writeRuleSetData(byte[] ruleSetData);

    /**
     * Checks the syntax of the current rule set.
     *
     * @return true if successful, false otherwise
     */
    public boolean checkSyntax();

    /**
     * Restarts the web server.
     *
     * @return true if successful, false otherwise
     */
    public boolean restart();

    /**
     * Creates a new health check for this server.
     *
     * @return health check
     */
    public HealthCheck newHealthCheck();
}
