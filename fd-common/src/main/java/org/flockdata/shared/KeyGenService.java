/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.shared;

import org.flockdata.helper.Base64;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Generates unique keys based on various algorithms.
 *
 * User: Mike Holdsworth
 * Since: 11/09/13
 */
@Service
public class KeyGenService {
    public enum METHOD {
        UUID, SNOWFLAKE, BASE64
    }

    public String getUniqueKey() {
        return getUniqueKey(METHOD.BASE64);
    }

    String getUniqueKey(METHOD method) {
        // Snowflake?
        if (method.equals(METHOD.SNOWFLAKE))
            return getSnowFlake();
        else if (method.equals(METHOD.BASE64))
            return Base64.format(UUID.randomUUID());
        else

        return getUUID();
    }

    //ToDo: Implement!
    private String getSnowFlake() {
        return getUUID();
    }

    private String getUUID() {
        return UUID.randomUUID().toString();
    }



}
