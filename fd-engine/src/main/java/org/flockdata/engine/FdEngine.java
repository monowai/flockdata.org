/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

import javax.annotation.PostConstruct;
import org.flockdata.services.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Java configuration of fd-engines graphWriting services
 *
 * @author mholdsworth
 * @tag Application
 * @since 15/12/2014
 */
// test and store are required for functional testing only. not sure how to scan for them
@SpringBootApplication(
    scanBasePackages = {"org.flockdata.company",
        "org.flockdata.engine", "org.flockdata.geography",
        "org.flockdata.authentication", "org.flockdata.engine.configure", "org.flockdata.integration"})

public class FdEngine {
  @Autowired
  private SchemaService schemaService;

  public static void main(String[] args) {
    try {
      SpringApplication.run(FdEngine.class, args);
    } catch (Exception e) {
      System.out.println(e.getLocalizedMessage());
    }

  }

  @PostConstruct
  public void ensureSystemIndexes() {
    schemaService.ensureSystemIndexes(null);

  }


}


