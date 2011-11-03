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
package com.edmunds.etm.agent;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Agent utilities.
 *
 * @author Ryan Holmes
 */
public final class AgentUtils {

    private AgentUtils() {
        // This class should never be instantiated.
    }

    /**
     * Creates a message digest for the specified rule set data.
     *
     * @param ruleSetData rule set data
     * @return message digest
     */
    public static String ruleSetDigest(byte[] ruleSetData) {
        return DigestUtils.md5Hex(ruleSetData);
    }
}
