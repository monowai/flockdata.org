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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * User: Mike Holdsworth
 * Since: 29/08/13
 */
@Configuration
@PropertySource(value = "version.properties",ignoreResourceNotFound = true)
public class VersionHelper {

    @Value("${info.build.version:na}")
    String version;

    @Value("${info.build.plan:na}")
    String plan;

    @Value("${info.build.number:na}")
    String build;

    public  String getFdVersion() {
        return version + " (" + (plan==null?"na":plan) + "/" + (build ==null?"na":build)+")";
    }
}
