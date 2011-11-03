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
 * Interface for health check listeners. The single method, {@link #onHealthCheckComplete(boolean)} will be called when
 * the health check is complete.
 *
 * @author Ryan Holmes
 */
public interface HealthCheckListener {
    /**
     * Called when the health check has completed.
     * <p/>
     * The health check is considered "complete" when either of two conditions are met: 1. A connection to the server
     * was successful or, 2. the timeout value was exceeded.
     *
     * @param alive true if the server is responding, false otherwise
     */
    void onHealthCheckComplete(boolean alive);
}
