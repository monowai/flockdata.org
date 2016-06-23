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

package org.flockdata.track.service;

import org.flockdata.model.Company;
import org.flockdata.model.Fortress;
import org.flockdata.registration.TagInputBean;

import java.util.Collection;

/**
 * User: mike
 * Date: 5/09/14
 * Time: 4:17 PM
 */
public interface SchemaService {

    Boolean ensureSystemIndexes(Company company);

    /**
     * Deletes the majority of the structural associations between a fortress and data in FlockData
     * Does not delete the DocumentType
     *
     * You probably want to be calling adminService.purge() which in turn calls this
     *
     * @param fortress
     */
    void purge(Fortress fortress);

    Boolean ensureUniqueIndexes(Collection<TagInputBean> tagInputs);

}
