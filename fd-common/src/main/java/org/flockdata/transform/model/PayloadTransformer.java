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

package org.flockdata.transform.model;

import java.util.Map;
import org.flockdata.helper.FlockException;
import org.flockdata.transform.entity.EntityPayloadTransformer;
import org.flockdata.transform.tag.TagPayloadTransformer;

/**
 * Interface to support transformation of Map data into a target object
 *
 * @author mholdsworth
 * @since 20/11/2013
 */
public interface PayloadTransformer {

  /**
   * @param map raw input data
   * @return transformed data
   * @throws FlockException transformation issues
   * @see EntityPayloadTransformer
   * @see TagPayloadTransformer
   */
  Map<String, Object> transform(Map<String, Object> map) throws FlockException;


}
