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

package org.flockdata.engine.profile.service;

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
import org.flockdata.profile.ContentValidationRequest;
import org.flockdata.profile.ContentValidationResult;
import org.flockdata.profile.ContentValidationResults;
import org.flockdata.profile.model.ContentProfile;
import org.flockdata.profile.service.ContentProfileService;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.shared.ClientConfiguration;
import org.flockdata.shared.FileProcessor;
import org.flockdata.track.service.FortressService;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.Transformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 2:43 PM
 */
@Service
public class ProfileServiceNeo implements ContentProfileService {

    @Autowired
    ProfileDaoNeo profileDao;

    @Autowired
    FortressService fortressService;

    @Autowired
    ConceptService conceptService;

    @Autowired
    FdServerWriter fdServerWriter;

    @Autowired
    FileProcessor fileProcessor;

    private static final ObjectMapper objectMapper = FdJsonObjectMapper.getObjectMapper();

    public ContentProfile get(Fortress fortress, DocumentType documentType) throws FlockException {
        Profile profile = profileDao.find(fortress, documentType);

        if (profile == null)
            throw new NotFoundException(String.format("Unable to locate and import profile for [%s], [%s]", fortress.getCode(), documentType.getName()));
        //return profile;
        String json = profile.getContent();
        try {
            ContentProfileImpl iProfile=  objectMapper.readValue(json, ContentProfileImpl.class);
            iProfile.setFortress(new FortressInputBean(fortress.getName()));
            iProfile.setDocumentName(documentType.getName());
            return iProfile;
        } catch (IOException e) {
            throw new FlockException(String.format("Unable to obtain content from ImportProfile {%d}", profile.getId()), e);
        }
    }

    public Profile save(Fortress fortress, DocumentType documentType, ContentProfile profileConfig) throws FlockException {

        Profile profile = profileDao.find(fortress, documentType);
        if (profile == null) {
            profile = new Profile(fortress, documentType);
        }
        try {
            profile.setContent(objectMapper.writeValueAsString(profileConfig));

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new FlockException("Json error", e);
        }

        // ToDo: Track this as a FlockData Entity to take advantage of versions
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

    public int process(Company company, Fortress fortress, DocumentType documentType, String file, Boolean async) throws FlockException, ClassNotFoundException, IOException, InstantiationException, IllegalAccessException {
        ContentProfile profile = get(fortress, documentType);
        // Users PUT params override those of the contentProfile
        if ( !profile.getFortress().getName().equalsIgnoreCase(fortress.getName()))
            profile.setFortress( new FortressInputBean(fortress.getName(), !fortress.isSearchEnabled()));
        if ( !profile.getDocumentType().getName().equalsIgnoreCase(documentType.getName()))
            profile.setDocumentName(documentType.getName());
        FileProcessor.validateArgs(file);
        ClientConfiguration defaults = new ClientConfiguration();
        defaults.setBatchSize(1);
        return fileProcessor.processFile(profile, file);
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

    @Override
    public ContentValidationResults validate(ContentValidationRequest contentRequest) {
        assert contentRequest!=null;
        assert contentRequest.getContentProfile() !=null;
        ContentValidationResults validatedContent = new ContentValidationResults();
        Map<String, ColumnDefinition> content = contentRequest.getContentProfile().getContent();
        for (String column : content.keySet()) {
            ContentValidationResult result = new ContentValidationResult(content.get(column), "ok");
            validatedContent.add(result);
        }
        return validatedContent;
    }

    @Override
    public ContentProfile createDefaultContentProfile(ContentValidationRequest contentRequest) {
        ContentProfileImpl result = new ContentProfileImpl();
        Collection<Map<String, Object>> content = contentRequest.getRows();
        Transformer.fromMapToProfile(contentRequest.getRows());




//        Map<String, ColumnDefinition> columns = new TreeMap<>();TransformationHelper.

        return result;
    }


}
