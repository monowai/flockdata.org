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

package org.flockdata.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.integration.config.EnableIntegration;

/**
 * Starts fd-search as a SB app
 *
 * @author mholdsworth
 * @tag Application, Search
 * @since 16/12/2014
 */
@SpringBootApplication(scanBasePackages = {
    "org.flockdata.search",
    "org.flockdata.integration"})
@EnableIntegration
public class FdSearch {

  public static void main(String[] args) {
    try {
      SpringApplication.run(FdSearch.class, args);
    } catch (Exception e) {
      System.out.println(e.getLocalizedMessage());
    }

  }


}
