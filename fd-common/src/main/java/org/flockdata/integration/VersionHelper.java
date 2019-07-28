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

package org.flockdata.integration;

import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

/**
 * Build properties
 *
 * @author mholdsworth
 * @tag Configuration, Version, GIT
 * @since 29/08/2013
 */
@Configuration
@Profile("fd-server")
@PropertySource(value = {"version.properties", "git.properties"}, ignoreResourceNotFound = true)
public class VersionHelper {

  @Value("${git.build.version:na}")
  String version;
  @Value("${info.build.plan:na}")
  String plan;
  @Value("${git.commit.id.abbrev}")
  String gitCommit;
  @Value("${git.branch:na}")
  String branch;
  @Value("${git.commit.message.short}")
  String commitMessage;
  private Logger logger = LoggerFactory.getLogger("configuration");

  @PostConstruct
  public void logVersion() {
    logger.info("**** " + getFdVersion());
    logger.debug(commitMessage);
  }

  public String getFdVersion() {
    if (version.contains("SNAPSHOT")) {
      return version + " (" + branch + "/" + gitCommit + ")";
    } else {
      return version + " (" + gitCommit + ")";
    }
  }
}
