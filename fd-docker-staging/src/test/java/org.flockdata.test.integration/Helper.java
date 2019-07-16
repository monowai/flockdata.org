/*
 *  Copyright 2012-2017 the original author or authors.
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

package org.flockdata.test.integration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mholdsworth
 * @since 19/04/2016
 */
public class Helper {
    //    private static final Logger logger = LoggerFactory.getLogger(Helper.class);
    static Map<String, Object> getSimpleMap(String key, Object value) {
        Map<String, Object> result = new HashMap<>();
        result.put(key, value);
        return result;
    }

    static Map<String, Object> getRandomMap() {
        return getSimpleMap("Key", "Test" + System.currentTimeMillis());
    }

}
