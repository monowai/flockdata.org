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

package org.flockdata.store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts fd-store KV store service
 *
 * @author mholdsworth
 * @tag Application
 * @since 17/02/2016
 */
@SpringBootApplication(scanBasePackages = {
    "org.flockdata.integration",
    "org.flockdata.store"})
public class FdStore {
  public static void main(String[] args) {
    try {
      SpringApplication.run(FdStore.class, args);
    } catch (Exception e) {
      System.out.println(e.getLocalizedMessage());
    }

  }
}
