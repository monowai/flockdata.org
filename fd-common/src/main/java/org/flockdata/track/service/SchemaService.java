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

package org.flockdata.track.service;

import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Fortress;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.model.DocumentType;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * User: mike
 * Date: 5/09/14
 * Time: 4:17 PM
 */
public interface SchemaService {

    Boolean ensureSystemIndexes(Company company);

    DocumentType resolveByDocCode(Fortress fortress, String documentType);

    DocumentType resolveByDocCode(Fortress fortress, String documentType, Boolean createIfMissing);

    void registerConcepts(Company company, Iterable<TrackResultBean> resultBeans);

    Set<DocumentResultBean> findConcepts(Company company, Collection<String> documents, boolean withRelationships);

    void createDocTypes(Iterable<EntityInputBean> headers, Fortress fortress);

    Collection<DocumentResultBean> getDocumentsInUse(Company company);

    void purge(Fortress fortress);

    boolean ensureUniqueIndexes(Company company, List<TagInputBean> tagInputs, Collection<String> existingIndexes);

}
