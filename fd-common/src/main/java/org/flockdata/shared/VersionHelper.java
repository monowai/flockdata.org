/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PostConstruct;

/**
 * User: Mike Holdsworth
 * Since: 29/08/13
 */
@Configuration
@Profile("fd-server")
@PropertySource(value = {"version.properties","git.properties"},ignoreResourceNotFound = true)
public class VersionHelper {

    private Logger logger = LoggerFactory.getLogger("configuration");

    @Value("${git.build.version:na}")
    String version;

    @Value("${info.build.plan:na}")
    String plan;

//    @Value("${info.build.number:na}")
//    String build;

//    @Value("${info.bamboo.build}:na")
//    String bamboo;

    @Value("${git.commit.id.abbrev}")
    String gitCommit;

    @Value("${git.branch:na}")
    String branch;

    @Value("${git.commit.message.short}")
    String commitMessage;

    @PostConstruct
    public void logVersion(){
        logger.info("**** " + getFdVersion());
        logger.debug (commitMessage);
    }
    public  String getFdVersion() {
        return version + " (" + branch + "/" + gitCommit +")" ;
    }
}
