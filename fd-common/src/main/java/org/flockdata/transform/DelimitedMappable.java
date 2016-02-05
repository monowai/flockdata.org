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

package org.flockdata.transform;

/**
 * User: Mike Holdsworth
 * Since: 25/01/14
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import org.flockdata.helper.FlockException;
import org.flockdata.profile.model.ContentProfile;
import org.flockdata.profile.model.Mappable;

import java.util.Map;

/**
 * Support class to handle mapping from one format to another format
 * User: Mike Holdsworth
 * Since: 13/10/13
 */
public interface DelimitedMappable extends Mappable {

    Map<String, Object> setData(String[] headerRow, String[] line, ContentProfile contentProfile) throws JsonProcessingException, FlockException;

}
