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

package org.flockdata.engine.track.service;

import org.flockdata.data.Company;
import org.flockdata.data.ContentModel;
import org.flockdata.data.Document;
import org.flockdata.data.Fortress;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.helper.FlockException;
import org.flockdata.model.ContentModelResult;
import org.flockdata.model.ContentValidationRequest;
import org.flockdata.model.ContentValidationResults;

import java.util.Collection;

/**
 * @author mholdsworth
 * @since 3/10/2014
 */
public interface ContentModelService {

    ContentModel get(Company company, Fortress fortress, Document documentType) throws FlockException ;

    ContentModelResult saveEntityModel(Company company, Fortress fortress, Document documentType, ContentModel contentModel) throws FlockException;

    ContentModelResult saveTagModel(CompanyNode company, String code, ContentModel profileConfig) throws FlockException;

    ContentModel get(Company company, String fortressCode, String documentName) throws FlockException;

    ContentValidationResults validate(ContentValidationRequest contentRequest) throws FlockException;

    ContentModel createDefaultContentModel(ContentValidationRequest contentRequest);

    Collection<ContentModelResult> find(CompanyNode company);

    ContentModelResult find(CompanyNode company, String key) throws FlockException;

    ContentModel getTagModel(CompanyNode company, String code) throws FlockException;

    void delete(CompanyNode company, String key);
}
