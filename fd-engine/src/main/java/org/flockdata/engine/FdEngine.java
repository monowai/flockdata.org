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

package org.flockdata.engine;

import org.flockdata.track.service.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

//import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Java configuration of fd-engines graphWriting servicee
 * <p>
 * User: mike
 * Date: 15/12/14
 * Time: 3:49 PM
 */
// test and store are required for functional testing only. not sure how to scan for them
@SpringBootApplication(
        scanBasePackages = {"org.flockdata.company",
                "org.flockdata.test.engine", "org.flockdata.store",
                "org.flockdata.engine", "org.flockdata.geography",
                "org.flockdata.authentication", "org.flockdata.shared"})

//@EnableDiscoveryClient
public class FdEngine {
    public static void main(String[] args) {
        SpringApplication.run(FdEngine.class, args);
    }

    @Autowired
    SchemaService schemaService;

    @PostConstruct
    public void ensureSystemIndexes() {
        schemaService.ensureSystemIndexes(null);

    }


}


