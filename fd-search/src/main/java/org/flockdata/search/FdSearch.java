/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

/**
 *
 * User: mike
 * Date: 16/12/14
 * Time: 7:36 AM
 */
@SpringBootApplication (scanBasePackages = "org.flockdata.search")
@PropertySource(value = "classpath:/application.yml,file:${fd.config}", ignoreResourceNotFound = true)
public class FdSearch {

    private Logger logger = LoggerFactory.getLogger(FdSearch.class);


    public static void main(String[] args) {
        SpringApplication.run(FdSearch.class, args);
    }


}
