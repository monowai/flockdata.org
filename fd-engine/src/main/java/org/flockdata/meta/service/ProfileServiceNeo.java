/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

package org.flockdata.meta.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flockdata.engine.dao.ProfileDaoNeo;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.engine.track.service.FdServerWriter;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.Company;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Fortress;
import org.flockdata.model.Profile;
import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.profile.model.ContentProfile;
import org.flockdata.profile.service.ImportProfileService;
import org.flockdata.track.service.FortressService;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 2:43 PM
 */
@Service
public class ProfileServiceNeo implements ImportProfileService {

    @Autowired
    ProfileDaoNeo profileDao;

    @Autowired
    FortressService fortressService;

    @Autowired
    ConceptService conceptService;

    @Autowired
    FdServerWriter fdServerWriter;

    static final ObjectMapper objectMapper = FdJsonObjectMapper.getObjectMapper();

    public ContentProfile get(Fortress fortress, DocumentType documentType) throws FlockException {
        Profile profile = profileDao.find(fortress, documentType);

        if (profile == null)
            throw new NotFoundException(String.format("Unable to locate and import profile for [%s], [%s]", fortress.getCode(), documentType.getCode()));
        //return profile;
        String json = profile.getContent();
        try {
            ContentProfileImpl iProfile=  objectMapper.readValue(json, ContentProfileImpl.class);
            iProfile.setFortressName(fortress.getName());
            iProfile.setDocumentName(documentType.getName());
            return iProfile;
        } catch (IOException e) {
            throw new FlockException(String.format("Unable to obtain content from ImportProfile {%d}", profile.getId()), e);
        }
    }

    public Profile save(Fortress fortress, DocumentType documentType, ContentProfile profileConfig) throws FlockException {
        //objectMapper.
        Profile profile = profileDao.find(fortress, documentType);
        if (profile == null) {
            profile = new Profile(fortress, documentType);
        }
        try {
            profile.setContent(objectMapper.writeValueAsString(profileConfig));

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new FlockException("Json error", e);
        }

        // ToDo: Track the change against a FlockData system account
        return profileDao.save(profile);
    }

    @Override
    public void save(Company company, String fortressCode, String documentCode, ContentProfileImpl profile) throws FlockException {
        Fortress fortress = fortressService.findByCode(company, fortressCode);
        DocumentType documentType = conceptService.resolveByDocCode(fortress, documentCode, false);
        if (documentType == null )
            throw new NotFoundException("Unable to resolve document type ");
        save(fortress, documentType, profile);
    }

    /**
     * Does not validate the arguments.
     */
    @Override
    @Async ("fd-track")
    public void processAsync(Company company, String fortressCode, String documentCode, String file) throws ClassNotFoundException, FlockException, InstantiationException, IOException, IllegalAccessException {
        process(company, fortressCode, documentCode, file, true);
    }

    @Override
    public void process(Company company, String fortressCode, String documentCode, String file, boolean async) throws FlockException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        Fortress fortress = fortressService.findByCode(company, fortressCode);
        DocumentType documentType = conceptService.resolveByDocCode(fortress, documentCode, false);
        if (documentType == null )
            throw new NotFoundException("Unable to resolve document type ");
        process(company, fortress, documentType, file, async);
    }

    public Long process(Company company, Fortress fortress, DocumentType documentType, String file, Boolean async) throws FlockException, ClassNotFoundException, IOException, InstantiationException, IllegalAccessException {
        ContentProfile profile = get(fortress, documentType);
        profile.setFortressName(fortress.getName());
        profile.setDocumentName(documentType.getName());
        FileProcessor fileProcessor = new FileProcessor();
        FileProcessor.validateArgs(file);
        ClientConfiguration defaults = new ClientConfiguration();
        defaults.setBatchSize(1);
        return fileProcessor.processFile(profile, file, fdServerWriter, company, defaults);
    }

    @Override
    public void validateArguments(Company company, String fortressCode, String documentCode, String fileName) throws NotFoundException, IOException {
        if ( !FileProcessor.validateArgs(fileName)) {
            throw new NotFoundException("Unable to process filename "+ fileName);
        }
        Fortress fortress = fortressService.findByCode(company, fortressCode);
        if ( fortress == null )
            throw new NotFoundException("Unable to locate the fortress " + fortressCode);
        DocumentType documentType = conceptService.resolveByDocCode(fortress, documentCode, false);
        if (documentType == null )
            throw new NotFoundException("Unable to resolve document type " + documentCode);


    }

    @Override
    public ContentProfile get(Company company, String fortressCode, String documentCode) throws FlockException {
        Fortress fortress = fortressService.findByCode(company, fortressCode);
        if ( fortress == null )
            throw new NotFoundException("Unable to locate the fortress " + fortressCode);
        DocumentType documentType = conceptService.resolveByDocCode(fortress, documentCode, false);
        if (documentType == null )
            throw new NotFoundException("Unable to resolve document type " + documentCode);

        return get(fortress, documentType);
    }


}
