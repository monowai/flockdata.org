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

package org.flockdata.profile.service;

import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.profile.ImportProfile;
import org.flockdata.profile.model.ProfileConfiguration;
import org.flockdata.model.Company;
import org.flockdata.model.Fortress;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Profile;

import java.io.IOException;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 2:45 PM
 */
public interface ImportProfileService {

    public ProfileConfiguration get(Fortress fortress, DocumentType documentType ) throws FlockException ;

    void save(Company company, String fortressCode, String documentName, ImportProfile profile) throws FlockException;

    public Profile save(Fortress fortress, DocumentType documentType, ProfileConfiguration profileConfig) throws FlockException;

    public void process(Company company, String fortressCode, String documentCode, String file, boolean async) throws FlockException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException;

    void processAsync(Company company, String fortressCode, String documentName, String file) throws ClassNotFoundException, FlockException, InstantiationException, IOException, IllegalAccessException;

    public void process(Company company, Fortress fortressCode, DocumentType documentName, String pathToBatch, Boolean async) throws FlockException, ClassNotFoundException, IOException, InstantiationException, IllegalAccessException;

    public void validateArguments(Company company, String fortressCode, String documentCode, String fileName) throws NotFoundException, IOException ;

    public ProfileConfiguration get(Company company, String fortressCode, String documentName) throws FlockException;
}
