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
package com.edmunds.etm.agent.haproxy;

import com.edmunds.etm.agent.api.AgentConfig;
import com.edmunds.etm.common.api.ControllerPaths;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the HA Proxy agent process. <p/>
 *
 * @author Ryan Holmes
 */
@Component
public class HaProxyConfig extends AgentConfig {

    // Default path of the configuration file
    private static final String DEFAULT_FILE_PATH = "/etc/haproxy/haproxy.cfg";

    // Default command to check configuration file syntax
    private static final String DEFAULT_SYNTAX_CHECK_COMMAND = "/usr/sbin/haproxy -f {FILE_PATH} -c";

    // Default command to start HA Proxy
    private static final String DEFAULT_START_COMMAND = "sudo /sbin/service haproxy start";

    // Default command to restart HA Proxy
    private static final String DEFAULT_RESTART_COMMAND = "sudo /sbin/service haproxy reload";

    public HaProxyConfig() {
        this.filePath = DEFAULT_FILE_PATH;
        this.syntaxCheckCommand = DEFAULT_SYNTAX_CHECK_COMMAND;
        this.startCommand = DEFAULT_START_COMMAND;
        this.restartCommand = DEFAULT_RESTART_COMMAND;
    }

    @Override
    public String getRuleSetNodePath(ControllerPaths controllerPaths) {
        return controllerPaths.getHaProxyConf();
    }
}
