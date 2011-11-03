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

import com.edmunds.etm.agent.api.HealthCheck;
import com.edmunds.etm.agent.api.HealthCheckListener;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A {@code HealthCheck} that performs a TCP connect at the given hostname and port number.
 * <p/>
 * It attempts to connect to the server every {@link #interval} milliseconds until it either connects successfully or
 * times out based on the value of {@link #timeout}, whichever occurs first.
 *
 * @author Ryan Holmes
 */
public class TcpHealthCheck implements HealthCheck {
    private static final Logger logger = Logger.getLogger(TcpHealthCheck.class);

    private final String hostName;
    private final int port;
    private final long interval;
    private final long timeout;

    private HealthCheckListener healthCheckListener;
    private long lastCheckTime;
    private long elapsedTime;
    private InetAddress hostAddress;

    /**
     * Constructs a new TcpHealthCheck with the given parameters.
     *
     * @param hostName server host name
     * @param port     server port number
     * @param interval polling interval in milliseconds
     * @param timeout  maximum wait time in milliseconds, after which the check is considered failed
     */
    public TcpHealthCheck(String hostName, int port, long interval, long timeout) {
        this.hostName = hostName;
        this.port = port;
        this.interval = interval;
        this.timeout = timeout;
    }

    @Override
    public void execute(HealthCheckListener listener) {

        // Ensure that the listener is set
        Validate.notNull(listener, "Health check listener is null");
        healthCheckListener = listener;

        // Initialize time values
        elapsedTime = 0;
        lastCheckTime = new Date().getTime();

        // Schedule the initial health check
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new HealthCheckTask(timer), interval, interval);

        if (logger.isInfoEnabled()) {
            InetAddress addr = getHostAddress();
            String message = String.format("Health check started for host %s on port %d", addr.getHostAddress(), port);
            logger.info(message);
        }
    }

    protected boolean connectToServer() {

        InetAddress addr = getHostAddress();

        boolean connected;
        try {
            Socket socket = new Socket(addr, port);
            socket.close();
            connected = true;
        } catch (IOException e) {
            connected = false;
        }

        if (logger.isDebugEnabled()) {
            String message = String.format("Connected to server with result: %b", connected);
            logger.debug(message);
        }

        return connected;
    }

    protected HealthCheckListener getListener() {
        return healthCheckListener;
    }

    private InetAddress getHostAddress() {
        if (hostAddress == null) {
            try {
                if (StringUtils.isEmpty(hostName)) {
                    hostAddress = InetAddress.getLocalHost();
                } else {
                    hostAddress = InetAddress.getByName(hostName);
                }
            } catch (UnknownHostException e) {
                String message = String.format("Could not get IP address for host %s", hostName);
                logger.error(message, e);
                throw new RuntimeException(message, e);
            }
        }
        return hostAddress;
    }

    /**
     * Timer task to perform the health check.
     */
    private class HealthCheckTask extends TimerTask {
        private Timer timer;

        public HealthCheckTask(Timer timer) {
            this.timer = timer;
        }

        @Override
        public void run() {

            // Perform health check
            boolean alive = connectToServer();

            // Update time statistics
            Date now = new Date();
            elapsedTime += now.getTime() - lastCheckTime;
            lastCheckTime = now.getTime();

            if (logger.isDebugEnabled()) {
                String message = String.format("Time statistics updated with lastCheckTime %d and elapsedTime %d",
                    lastCheckTime,
                    elapsedTime);
                logger.debug(message);
            }

            // Notify the listener if the check succeeds or if we've exceeded the timeout
            if (alive || elapsedTime > timeout) {
                if (logger.isInfoEnabled()) {
                    String message = String.format("Health check completed with result: %b", alive);
                    logger.info(message);
                }
                timer.cancel();
                getListener().onHealthCheckComplete(alive);
            }
        }
    }
}
