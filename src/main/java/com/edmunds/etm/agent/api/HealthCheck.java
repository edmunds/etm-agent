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
 * Executes a health check to determine if a server is responding.
 * <p/>
 * Call the {@link #execute(HealthCheckListener)} method to begin the health check. Upon a successful connection or a
 * timeout, the {@link HealthCheckListener#onHealthCheckComplete(boolean)} method will be called on the listener with a
 * boolean value indicating success or failure.
 */
public interface HealthCheck {

    public void execute(HealthCheckListener listener);
}
