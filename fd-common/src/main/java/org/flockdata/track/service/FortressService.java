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

import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.*;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.FortressResultBean;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.EntityInputBean;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

/**
 * User: mike
 * Date: 14/11/14
 * Time: 12:21 PM
 */
public interface FortressService {
    Fortress getFortress(Long id);

    FortressUser getUser(Long id);

    //    @Cacheable(value = "fortressName", unless = "#result == null")
    Fortress findByName(Company company, String fortressName) throws NotFoundException;

    Fortress findByName(String fortressName) throws NotFoundException;

    Fortress findByCode(String fortressCode);

    Fortress findByCode(Company company, String fortressCode);

    FortressUser getFortressUser(Company company, String fortressName, String fortressUser) throws NotFoundException;

    FortressUser getFortressUser(Fortress fortress, String fortressUser);

    FortressUser getFortressUser(Fortress fortress, String fortressUser, boolean createIfMissing);

    Collection<FortressResultBean> findFortresses() throws FlockException;

    Collection<FortressResultBean> findFortresses(Company company) throws FlockException;

    void purge(Fortress fortress) throws FlockException;

    Fortress registerFortress(Company company, FortressInputBean fortressInputBean);

    Fortress registerFortress(Company company, String fortressName);

    Fortress registerFortress(Company company, FortressInputBean fib, boolean createIfMissing);

    Collection<DocumentResultBean> getFortressDocumentsInUse(Company company, String code) throws NotFoundException;

    Fortress getFortress(Company company, String fortressName) throws NotFoundException;

    String delete(Company company, String fortressName);

    Future<Void> createFortressUsers(Fortress fortress, List<EntityInputBean> inputBeans);

    FortressUser createFortressUser(Fortress fortress, ContentInputBean inputBean);

    String getGeoQuery(Entity entity);

    EntityService.TAG_STRUCTURE getTagStructureFinder(Entity entity);

    FortressSegment getDefaultSegment (Fortress fortress);

    FortressSegment addSegment(FortressSegment segment);

    Collection<FortressSegment> getSegments(Fortress fortress);

    FortressSegment resolveSegment(Company company, FortressInputBean fortress, String segmentName, String timeZone) throws NotFoundException;

    Fortress updateFortress(Company company, Fortress existing, FortressInputBean fortressInputBean);

    Fortress findInternalFortress(Company company);
}
