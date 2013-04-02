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

import com.edmunds.etm.common.api.ControllerPaths;
import org.apache.commons.lang.Validate;

/**
 * Configuration properties for the agent process. <p/>
 *
 * @author David Trott
 */
public abstract class AgentConfig {

    // Token to denote the configuration file path (used in the syntax check command)
    private static final String FILE_PATH_TOKEN_REGEX = "\\{FILE_PATH\\}";

    // Default encoding of the configuration file
    private static final String DEFAULT_FILE_ENCODING = "UTF-8";

    // Default server host name
    private static final String DEFAULT_HOST_NAME = "";

    // Default server port
    private static final int DEFAULT_PORT = 80;

    // Default polling interval for health check
    private static final long DEFAULT_CHECK_INTERVAL = 1000;

    // Default maximum wait time for health check
    private static final long DEFAULT_CHECK_TIMEOUT = 10000;

    // Fully qualified path to the configuration file
    protected String filePath;

    // Encoding of the configuration file
    protected String fileEncoding;

    // Command to check the configuration file syntax
    protected String syntaxCheckCommand;

    // Command to start the external proxy
    protected String startCommand;

    // Command to restart the external proxy
    protected String restartCommand;

    // Server host name
    protected String hostName;

    // Server port
    protected int port;

    // Health check interval in milliseconds
    protected long checkInterval;

    // Health check timeout in milliseconds
    protected long checkTimeout;

    public AgentConfig() {
        this.fileEncoding = DEFAULT_FILE_ENCODING;
        this.hostName = DEFAULT_HOST_NAME;
        this.port = DEFAULT_PORT;
        this.checkInterval = DEFAULT_CHECK_INTERVAL;
        this.checkTimeout = DEFAULT_CHECK_TIMEOUT;
    }

    /**
     * Get the fully qualified path to the external proxy configuration file.
     *
     * @return configuration file path
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Sets the fully qualified path to the external proxy configuration file.
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
     * @return the external proxy syntax check command
     */
    public String getSyntaxCheckCommand() {
        return replaceFilePathToken(syntaxCheckCommand);
    }

    /**
     * Sets the command to check the syntax of the configuration file.
     *
     * @param syntaxCheckCommand the external proxy syntax check command
     */
    public void setSyntaxCheckCommand(String syntaxCheckCommand) {
        this.syntaxCheckCommand = syntaxCheckCommand;
    }

    /**
     * Gets the command to start the external proxy.
     *
     * @return the external proxy start command.
     */

    public String getStartCommand() {
        return startCommand;
    }

    /**
     * Sets the command to start the external proxy.
     *
     * @param startCommand the external proxy start command
     */
    public void setStartCommand(String startCommand) {
        this.startCommand = startCommand;
    }

    /**
     * Gets the command to restart the external proxy.
     * <p/>
     * The default is {@code sudo /sbin/service httpd reload}.
     *
     * @return the external proxy restart command
     */
    public String getRestartCommand() {
        return restartCommand;
    }

    /**
     * Sets the command to restart the external proxy.
     *
     * @param restartCommand the external proxy restart command
     */
    public void setRestartCommand(String restartCommand) {
        this.restartCommand = restartCommand;
    }

    /**
     * Gets the host name of the external proxy server.
     * <p/>
     * This value is used for health checks. The default is blank.
     *
     * @return the server host name
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Sets the host name of the external proxy server.
     *
     * @param hostName the server host name
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * Gets the port number of the external proxy server.
     * <p/>
     * This value is used for health checks. The default is {@code 80}.
     *
     * @return the server port
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the port number of the external proxy server.
     *
     * @param port the server port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Gets the health check interval in milliseconds.
     * <p/>
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
     * <p/>
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
     * Returns the ZooKeeper node path where the configuration for the external process is stored.
     *
     * @param controllerPaths the controller paths object used to lookup the path.
     * @return the ZooKeeper path.
     */
    public abstract String getRuleSetNodePath(ControllerPaths controllerPaths);

    /**
     * Validates the external proxy configuration.
     */
    public void validate() {
        Validate.notEmpty(filePath, "Configuration file path not specified");
        Validate.notEmpty(fileEncoding, "Configuration file encoding not specified");
        Validate.notEmpty(syntaxCheckCommand, "Syntax check command not specified");
        Validate.notEmpty(restartCommand, "Restart command not specified");
        Validate.notNull(hostName, "Host name not specified");
        Validate.isTrue(port > 0, "Port must be greater than zero");
        Validate.isTrue(checkInterval > 0, "Health check interval must be greater than zero");
        Validate.isTrue(checkTimeout > 0, "Health check timeout must be greater than zero");
    }

    private String replaceFilePathToken(String value) {
        return value.replaceAll(FILE_PATH_TOKEN_REGEX, filePath);
    }
}
