/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

package org.flockdata.profile.service;

import org.flockdata.helper.FlockException;
import org.flockdata.model.Company;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Fortress;
import org.flockdata.profile.ContentModelResult;
import org.flockdata.profile.ContentValidationRequest;
import org.flockdata.profile.ContentValidationResults;
import org.flockdata.profile.model.ContentModel;

import java.util.Collection;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 2:45 PM
 */
public interface ContentModelService {

    ContentModel get(Company company, Fortress fortress, DocumentType documentType) throws FlockException ;

    ContentModelResult saveEntityModel(Company company, Fortress fortress, DocumentType documentType, ContentModel contentModel) throws FlockException;

    ContentModelResult saveTagModel(Company company, String code, ContentModel profileConfig) throws FlockException;

    ContentModel get(Company company, String fortressCode, String documentName) throws FlockException;

    ContentValidationResults validate(ContentValidationRequest contentRequest) throws FlockException;

    ContentModel createDefaultContentModel(ContentValidationRequest contentRequest);

    Collection<ContentModelResult> find(Company company);

    ContentModelResult find(Company company, String key) throws FlockException;

    ContentModel getTagModel(Company company, String code) throws FlockException;

    void delete(Company company, String key);
}
