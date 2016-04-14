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
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.Company;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Fortress;
import org.flockdata.model.Profile;
import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.profile.model.ContentProfile;

import java.io.IOException;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 2:45 PM
 */
public interface ContentProfileService {

    ContentProfile get(Fortress fortress, DocumentType documentType) throws FlockException ;

    void save(Company company, String fortressCode, String documentName, ContentProfileImpl profile) throws FlockException;

    Profile save(Fortress fortress, DocumentType documentType, ContentProfile profileConfig) throws FlockException;

    void process(Company company, String fortressCode, String documentCode, String file, boolean async) throws FlockException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException;

    void processAsync(Company company, String fortressCode, String documentName, String file) throws ClassNotFoundException, FlockException, InstantiationException, IOException, IllegalAccessException;

    Long process(Company company, Fortress fortressCode, DocumentType documentName, String pathToBatch, Boolean async) throws FlockException, ClassNotFoundException, IOException, InstantiationException, IllegalAccessException;

    void validateArguments(Company company, String fortressCode, String documentCode, String fileName) throws NotFoundException, IOException ;

    ContentProfile get(Company company, String fortressCode, String documentName) throws FlockException;
}
