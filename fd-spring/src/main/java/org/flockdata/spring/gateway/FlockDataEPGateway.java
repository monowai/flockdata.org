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

package org.flockdata.spring.gateway;

import org.springframework.integration.annotation.Gateway;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: nabil
 * Date: 13/08/13
 * Time: 01:26
 * To change this template use File | Settings | File Templates.
 */
public interface FlockDataEPGateway {

    @Gateway(requestChannel = "pingChannel")
    String get();

    @Gateway(requestChannel = "healthChannel")
    Map<String, String> getHealth();
}
