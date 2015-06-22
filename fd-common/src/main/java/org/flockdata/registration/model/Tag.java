/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

package org.flockdata.registration.model;

import org.flockdata.track.model.Alias;

import java.util.Map;
import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 15/06/13
 * Time: 9:11 PM
 */
public interface Tag {

    String DEFAULT_TAG="Tag";
    String DEFAULT=":" + DEFAULT_TAG ;
    String UNDEFINED = "undefined";

    String getName();

    void setName(String name);

    Long getId();

    String getKey();

    Object getProperty(String num);

    Map<String, Object> getProperties();

    String getCode();

    String getLabel();

    boolean hasAlias(String theLabel, String code);

    Set<Alias> getAliases();

    Tag getLocated();

    boolean isDefault();
}
