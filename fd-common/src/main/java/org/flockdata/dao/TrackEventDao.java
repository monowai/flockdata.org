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

package org.flockdata.dao;

import org.flockdata.model.ChangeEvent;
import org.flockdata.model.Company;

import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 28/06/13
 * Time: 9:55 PM
 */
public interface TrackEventDao {

    ChangeEvent createEvent(Company company, String eventCode);

    Set<ChangeEvent> findCompanyEvents(Long id);
}
