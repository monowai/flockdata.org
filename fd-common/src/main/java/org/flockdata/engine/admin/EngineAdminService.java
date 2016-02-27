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

package org.flockdata.engine.admin;

import org.flockdata.helper.FlockException;
import org.flockdata.model.Company;
import org.flockdata.model.Entity;
import org.flockdata.model.Fortress;

import java.util.Collection;
import java.util.concurrent.Future;

/**
 * Created by mike on 25/03/15.
 */
public interface EngineAdminService {
    Future<Long> doReindex(Fortress fortress) throws FlockException;

    Future<Long> doReindex(Fortress fortress, String docType) throws FlockException;

    Future<Long> doReindex(Fortress fortress, Entity entity) throws FlockException;

    Future<Boolean> purge(Company company, Fortress fortress) throws FlockException ;

    Future<Collection<String>> validateFromSearch(Company company, Fortress fortress, String docType) throws FlockException;

    void doReindex(Entity entity) throws FlockException;
}
