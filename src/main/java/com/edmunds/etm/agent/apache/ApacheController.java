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

import com.edmunds.etm.agent.api.HealthCheck;
import com.edmunds.etm.agent.api.WebServerController;
import com.edmunds.etm.agent.impl.TcpHealthCheck;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

/**
 * Controller for an Apache web server.
 *
 * @author Ryan Holmes
 */
@Component
public class ApacheController implements WebServerController {

    private static final Logger logger = Logger.getLogger(ApacheController.class);

    // Process exit value indicating success
    private static final int PROCESS_SUCCESS_EXIT_VALUE = 0;

    private final ApacheConfig apacheConfig;

    @Autowired
    public ApacheController(ApacheConfig apacheConfig) {
        this.apacheConfig = apacheConfig;
        apacheConfig.validate();
    }

    @Override
    public byte[] readRuleSetData() {
        File configFile = new File(apacheConfig.getFilePath());

        byte[] configData = new byte[0];

        if (!configFile.exists()) {
            return configData;
        }

        try {
            configData = FileUtils.readFileToByteArray(configFile);
        } catch (IOException e) {
            String message = String
                .format("Could not read Apache configuration file at path %s", apacheConfig.getFilePath());
            logger.error(message, e);
        }
        return configData;
    }

    @Override
    public void writeRuleSetData(byte[] ruleSetData) {
        File configFile = new File(apacheConfig.getFilePath());
        try {
            FileUtils.writeByteArrayToFile(configFile, ruleSetData);
        } catch (IOException e) {
            String message = String
                .format("Could not write Apache configuration file at path %s", apacheConfig.getFilePath());
            logger.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    @Override
    public boolean checkSyntax() {
        Process child;
        try {
            child = Runtime.getRuntime().exec(apacheConfig.getSyntaxCheckCommand());
            child.waitFor();
        } catch (IOException e) {
            String message = "Could not execute Apache syntax check";
            logger.error(message, e);
            return false;
        } catch (InterruptedException e) {
            String message = "Thread interrupted while waiting for Apache syntax check";
            logger.error(message, e);
            return false;
        }

        return child.exitValue() == PROCESS_SUCCESS_EXIT_VALUE;
    }

    @Override
    public boolean restart() {
        Process child;
        try {
            child = Runtime.getRuntime().exec(apacheConfig.getRestartCommand());
            child.waitFor();
        } catch (IOException e) {
            String message = "Could not execute Apache restart command";
            logger.error(message, e);
            return false;
        } catch (InterruptedException e) {
            String message = "Thread interrupted while waiting for Apache restart";
            logger.error(message, e);
            return false;
        }

        return child.exitValue() == PROCESS_SUCCESS_EXIT_VALUE;
    }

    @Override
    public HealthCheck newHealthCheck() {
        return new TcpHealthCheck(apacheConfig.getHostName(),
            apacheConfig.getPort(),
            apacheConfig.getCheckInterval(),
            apacheConfig.getCheckTimeout());
    }
}
