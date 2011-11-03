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

import org.apache.commons.lang.Validate;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Apache agent process. <p/>
 *
 * @author Ryan Holmes
 */
@Component
public class ApacheConfig {

    // Default path of the configuration file
    private static final String DEFAULT_FILE_PATH = "/var/lib/etm-agent/etm-apache.conf";

    // Token to denote the configuration file path (used in the syntax check command)
    private static final String FILE_PATH_TOKEN_REGEX = "\\{FILE_PATH\\}";

    // Default encoding of the configuration file
    private static final String DEFAULT_FILE_ENCODING = "UTF-8";

    // Default command to check configuration file syntax
    private static final String DEFAULT_SYNTAX_CHECK_COMMAND = "/apps/apache-httpd/bin/apachectl -t -f {FILE_PATH}";

    // Default command to restart Apache
    private static final String DEFAULT_RESTART_COMMAND = "sudo /sbin/service httpd restart";

    // Default server host name
    private static final String DEFAULT_HOST_NAME = "";

    // Default server port
    private static final int DEFAULT_PORT = 80;

    // Default polling interval for health check
    private static final long DEFAULT_CHECK_INTERVAL = 1000;

    // Default maximum wait time for health check
    private static final long DEFAULT_CHECK_TIMEOUT = 10000;

    // Fully qualified path to the configuration file
    private String filePath = DEFAULT_FILE_PATH;

    // Encoding of the configuration file
    private String fileEncoding = DEFAULT_FILE_ENCODING;

    // Command to check the configuration file syntax
    private String syntaxCheckCommand = DEFAULT_SYNTAX_CHECK_COMMAND;

    // Command to restart Apache
    private String restartCommand = DEFAULT_RESTART_COMMAND;

    // Server host name
    private String hostName = DEFAULT_HOST_NAME;

    // Server port
    private int port = DEFAULT_PORT;

    // Health check interval in milliseconds
    private long checkInterval = DEFAULT_CHECK_INTERVAL;

    // Health check timeout in milliseconds
    private long checkTimeout = DEFAULT_CHECK_TIMEOUT;

    /**
     * Get the fully qualified path to the Apache configuration file.
     *
     * The default path is {@code /var/lib/etm-agent/etm-apache.conf}
     *
     * @return configuration file path
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Sets the fully qualified path to the Apache configuration file.
     *
     * @param filePath configuration file path
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Gets the encoding of the configuration file (default: UTF-8).
     *
     * @return configuration file encoding
     */
    public String getFileEncoding() {
        return fileEncoding;
    }

    /**
     * Sets the encoding of the configuration file.
     *
     * @param fileEncoding configuration file encoding
     */
    public void setFileEncoding(String fileEncoding) {
        this.fileEncoding = fileEncoding;
    }

    /**
     * Gets the command to check the syntax of the configuration file.
     *
     * The default is {@code /apps/apache-httpd/bin/apachectl -t -f {FILE_PATH}}.
     *
     * @return the Apache syntax check command
     */
    public String getSyntaxCheckCommand() {
        return replaceFilePathToken(syntaxCheckCommand);
    }

    /**
     * Sets the command to check the syntax of the configuration file.
     *
     * @param syntaxCheckCommand the Apache syntax check command
     */
    public void setSyntaxCheckCommand(String syntaxCheckCommand) {
        this.syntaxCheckCommand = syntaxCheckCommand;
    }

    /**
     * Gets the command to restart Apache.
     *
     * The default is {@code sudo /sbin/service httpd restart}.
     *
     * @return the Apache restart command
     */
    public String getRestartCommand() {
        return restartCommand;
    }

    /**
     * Sets the command to restart Apache.
     *
     * @param restartCommand the Apache restart command
     */
    public void setRestartCommand(String restartCommand) {
        this.restartCommand = restartCommand;
    }

    /**
     * Gets the host name of the Apache server.
     *
     * This value is used for health checks. The default is blank.
     *
     * @return the server host name
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Sets the host name of the Apache server.
     *
     * @param hostName the server host name
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * Gets the port number of the Apache server.
     *
     * This value is used for health checks. The default is {@code 80}.
     *
     * @return the server port
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the port number of the Apache server.
     *
     * @param port the server port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Gets the health check interval in milliseconds.
     *
     * This value controls how often (and therefore, how many) health checks are performed up to the timeout specified
     * by {@link #getCheckTimeout()}. The default is {@code 1000} (one per second).
     *
     * @return health check interval in milliseconds
     */
    public long getCheckInterval() {
        return checkInterval;
    }

    /**
     * Sets the health check interval in milliseconds.
     *
     * @param checkInterval health check interval in milliseconds
     */
    public void setCheckInterval(long checkInterval) {
        this.checkInterval = checkInterval;
    }

    /**
     * Gets the health check timeout in milliseconds.
     *
     * This value controls the total amount of time the health check will be performed before determining that the
     * server is not responding. The default is {@code 10000} (ten seconds).
     *
     * @return health check timeout in milliseconds
     */
    public long getCheckTimeout() {
        return checkTimeout;
    }

    /**
     * Sets the health check interval in milliseconds.
     *
     * @param checkTimeout health check timeout in milliseconds
     */
    public void setCheckTimeout(long checkTimeout) {
        this.checkTimeout = checkTimeout;
    }

    /**
     * Validates the Apache configuration.
     */
    public void validate() {
        Validate.notEmpty(filePath, "Apache configuration file path not specified");
        Validate.notEmpty(fileEncoding, "Apache configuration file encoding not specified");
        Validate.notEmpty(syntaxCheckCommand, "Apache syntax check command not specified");
        Validate.notEmpty(restartCommand, "Apache restart command not specified");
        Validate.notNull(hostName, "Apache host name not specified");
        Validate.isTrue(port > 0, "Apache port must be greater than zero");
        Validate.isTrue(checkInterval > 0, "Health check interval must be greater than zero");
        Validate.isTrue(checkTimeout > 0, "Health check timeout must be greater than zero");
    }

    private String replaceFilePathToken(String value) {
        return value.replaceAll(FILE_PATH_TOKEN_REGEX, filePath);
    }

}
