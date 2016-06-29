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

    ContentValidationResults validate(ContentValidationRequest contentRequest);

    ContentModel createDefaultContentModel(ContentValidationRequest contentRequest);

    Collection<ContentModelResult> find(Company company);

    ContentModelResult find(Company company, String key) throws FlockException;

    ContentModel getTagModel(Company company, String code) throws FlockException;
}
