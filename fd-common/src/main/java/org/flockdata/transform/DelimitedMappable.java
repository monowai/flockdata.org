/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
