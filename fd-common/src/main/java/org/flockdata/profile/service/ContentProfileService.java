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
import org.flockdata.profile.ContentProfileResult;
import org.flockdata.profile.ContentValidationRequest;
import org.flockdata.profile.ContentValidationResults;
import org.flockdata.profile.model.ContentProfile;

import java.util.Collection;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 2:45 PM
 */
public interface ContentProfileService {

    ContentProfile get(Company company, Fortress fortress, DocumentType documentType) throws FlockException ;

    ContentProfileResult saveFortressContentType(Company company, Fortress fortress, DocumentType documentType, ContentProfile profileConfig) throws FlockException;

    ContentProfile get(Company company, String fortressCode, String documentName) throws FlockException;

    ContentValidationResults validate(ContentValidationRequest contentRequest);

    ContentProfile createDefaultContentProfile(ContentValidationRequest contentRequest);

    Collection<ContentProfileResult> find(Company company);

    ContentProfileResult find(Company company, String key);
}
