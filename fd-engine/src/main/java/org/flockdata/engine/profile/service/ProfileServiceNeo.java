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
import org.flockdata.engine.configure.SecurityHelper;
import org.flockdata.engine.dao.ProfileDaoNeo;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.JsonUtils;
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.Company;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Fortress;
import org.flockdata.model.Profile;
import org.flockdata.profile.*;
import org.flockdata.profile.model.ContentProfile;
import org.flockdata.profile.service.ContentProfileService;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.service.EntityService;
import org.flockdata.track.service.FortressService;
import org.flockdata.track.service.MediationFacade;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.Transformer;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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


    private static final ObjectMapper objectMapper = FdJsonObjectMapper.getObjectMapper();

    public ContentProfile get(Company company, Fortress fortress, DocumentType documentType) throws FlockException {
        Profile profile = profileDao.find(fortress, documentType);

        if (profile == null)
            throw new NotFoundException(String.format("Unable to locate and import profile for [%s], [%s]", fortress.getCode(), documentType.getName()));

        // Serialized content profile is stored in a log. Here we retrieve the last saved one
        // but we could return the entire history
        Map<String,Object> data = entityService.getEntityDataLast(company, profile.getKey());
        String json = JsonUtils.toJson(data);

        try {
            ContentProfileImpl iProfile = objectMapper.readValue(json, ContentProfileImpl.class);
            iProfile.setFortress(new FortressInputBean(fortress.getName()));
            iProfile.setDocumentName(documentType.getName());
            return iProfile;
        } catch (IOException e) {
            throw new FlockException(String.format("Unable to obtain content from ImportProfile {%d}", profile.getId()), e);
        }
    }

    @Autowired
    MediationFacade mediationFacade;

    @Autowired
    SecurityHelper securityHelper;

    @Autowired
    EntityService entityService;

    /**
     * Identifies the ContentProfile as being a belonging to a specific Fortress/Document combo
     *
     * @param company
     * @param fortress
     * @param documentType
     * @param profileConfig
     * @return
     * @throws FlockException
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws IOException
     */
    @Transactional
    public ContentProfileResult saveFortressContentType(Company company, Fortress fortress, DocumentType documentType, ContentProfile profileConfig) throws FlockException {
        // Used for storing internal versionable data
        Fortress internalFortress = fortressService.findInternalFortress(company);
        assert internalFortress != null;

        Profile existingProfile = profileDao.find(fortress, documentType);
        try {
            if (existingProfile == null) {
                EntityInputBean entityInputBean = new EntityInputBean(internalFortress, "FdContentProfile");
                entityInputBean.setName(profileConfig.getName());
                ContentInputBean contentInputBean = new ContentInputBean(securityHelper.getLoggedInUser(), new DateTime());
                Map<String,Object> map= JsonUtils.convertToMap(profileConfig);

                contentInputBean.setData(map);
                entityInputBean.setContent(contentInputBean);
                TrackResultBean trackResult = mediationFacade.trackEntity(company, entityInputBean);
                existingProfile = new Profile(trackResult, fortress, documentType);
                profileDao.save(existingProfile);
            } else {
                ContentInputBean contentInputBean = new ContentInputBean(securityHelper.getLoggedInUser(), new DateTime());
                contentInputBean.setKey(existingProfile.getKey());
                contentInputBean.setData(JsonUtils.convertToMap(profileConfig));
                mediationFacade.trackLog(company, contentInputBean);
                if (profileConfig.getName() != null && !profileConfig.getName().equals(existingProfile.getName())){
                    existingProfile.setName(profileConfig.getName());
                    profileDao.save(existingProfile);

                }
            }
        } catch (ExecutionException | InterruptedException | IOException e) {
            throw new FlockException(e.getMessage());
        }

        return new ContentProfileResult(existingProfile);
    }

    @Override
    public ContentProfile get(Company company, String fortressCode, String documentCode) throws FlockException {
        Fortress fortress = fortressService.findByCode(company, fortressCode);
        if (fortress == null)
            throw new NotFoundException("Unable to locate the fortress " + fortressCode);
        DocumentType documentType = conceptService.resolveByDocCode(fortress, documentCode, false);
        if (documentType == null)
            throw new NotFoundException("Unable to resolve document type " + documentCode);

        return get(company, fortress, documentType);
    }


    @Override
    @Transactional
    public Collection<ContentProfileResult> find(Company company) {
        return profileDao.find(company.getId());
    }

    @Override
    @Transactional
    public ContentProfileResult find(Company company, String key) {
        ContentProfileResult result = profileDao.findByKey(company.getId(), key);
        if (result == null) {
            throw new NotFoundException("Unable to locate ContentProfile with key " + key);
        }
        return result;
    }

    @Override
    public ContentValidationResults validate(ContentValidationRequest contentRequest) {
        assert contentRequest != null;
        assert contentRequest.getContentProfile() != null;
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
        result.setContent(Transformer.fromMapToProfile(contentRequest.getRows()));
        return result;
    }





}
