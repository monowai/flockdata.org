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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flockdata.engine.configure.SecurityHelper;
import org.flockdata.engine.dao.ContentModelDaoNeo;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.helper.*;
import org.flockdata.model.Company;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Fortress;
import org.flockdata.model.Model;
import org.flockdata.profile.*;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.profile.service.ContentModelService;
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
public class ContentModelServiceNeo implements ContentModelService {

    @Autowired
    ContentModelDaoNeo contentModelDao;

    @Autowired
    FortressService fortressService;

    @Autowired
    ConceptService conceptService;


    private static final ObjectMapper objectMapper = new ObjectMapper( new FdJsonObjectMapper())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(JsonParser.Feature.ALLOW_COMMENTS);

    public ContentModel get(Company company, Fortress fortress, DocumentType documentType) throws FlockException {
        Model model = contentModelDao.find(fortress, documentType);

        if (model == null)
            throw new NotFoundException(String.format("Unable to locate and import model for [%s], [%s]", fortress.getCode(), documentType.getName()));

        // Serialized content profile is stored in a log. Here we retrieve the last saved one
        // but we could return the entire history
        ContentModel contentModel = getContentModel(company, model.getKey());
        if (contentModel != null) {
            contentModel.setFortress(new FortressInputBean(fortress.getName()));
            contentModel.setDocumentName(documentType.getName());
        }
        return contentModel;
    }

    private ContentModel getContentModel(Company company, String profileKey) throws FlockException {
        Map<String, Object> data = entityService.getEntityDataLast(company, profileKey);
        String json = JsonUtils.toJson(data);
        ContentModel contentModel;
        try {
            contentModel = objectMapper.readValue(json, ContentModelHandler.class);
        } catch (IOException e) {
            throw new FlockException(String.format("Unable to obtain content from ImportProfile {%s}", profileKey), e);
        }
        return contentModel;
    }

    @Autowired
    MediationFacade mediationFacade;

    @Autowired
    SecurityHelper securityHelper;

    @Autowired
    EntityService entityService;

    @Transactional
    public ContentModelResult saveTagModel(Company company, String code, ContentModel contentModel) throws FlockException {
        Fortress internalFortress = fortressService.findInternalFortress(company);

        assert internalFortress != null;

        String profileCode = TagHelper.parseKey(code);
        contentModel.setTagModel(true);

        Model existingModel = contentModelDao.findTagProfile(company, profileCode);
        try {
            if (existingModel == null) {
                EntityInputBean entityInputBean = new EntityInputBean(internalFortress, "TagModel");
                entityInputBean.setCode(code);
                entityInputBean.setName(contentModel.getName());
                ContentInputBean contentInputBean = new ContentInputBean(securityHelper.getLoggedInUser(), new DateTime());
                Map<String, Object> map = JsonUtils.convertToMap(contentModel);

                contentInputBean.setData(map);
                entityInputBean.setContent(contentInputBean);
                TrackResultBean trackResult = mediationFacade.trackEntity(company, entityInputBean);

                existingModel = new Model(trackResult, profileCode);
                contentModelDao.save(existingModel);
            } else {
                updateProfile(company, contentModel.setTagModel(true), existingModel);
            }
        } catch (ExecutionException | InterruptedException | IOException e) {
            throw new FlockException(e.getMessage());
        }

        return new ContentModelResult(existingModel);

    }

    private void updateProfile(Company company, org.flockdata.profile.model.ContentModel contentModelConfig, Model existingModel) throws FlockException, IOException, ExecutionException, InterruptedException {
        ContentInputBean contentInputBean = new ContentInputBean(securityHelper.getLoggedInUser(), new DateTime());
        contentInputBean.setKey(existingModel.getKey());
        contentInputBean.setData(JsonUtils.convertToMap(contentModelConfig));
        mediationFacade.trackLog(company, contentInputBean);
        if (contentModelConfig.getName() != null && !contentModelConfig.getName().equals(existingModel.getName())) {
            existingModel.setName(contentModelConfig.getName());
            contentModelDao.save(existingModel);

        }
    }

    /**
     * Identifies the ContentProfile as being a belonging to a specific Fortress/Document combo
     *
     * @param company
     * @param fortress
     * @param documentType
     * @param contentModel
     * @return
     * @throws FlockException
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws IOException
     */
    @Transactional
    public ContentModelResult saveEntityModel(Company company, Fortress fortress, DocumentType documentType, ContentModel contentModel) throws FlockException {
        // Used for storing internal versionable data
        Fortress internalFortress = fortressService.findInternalFortress(company);
        assert internalFortress != null;

        Model model = contentModelDao.find(fortress, documentType);
        try {
            if (model == null) {
                EntityInputBean entityInputBean = new EntityInputBean(internalFortress, "FdContentModel");
                entityInputBean.setName(contentModel.getName());
                ContentInputBean contentInputBean = new ContentInputBean(securityHelper.getLoggedInUser(), new DateTime());
                Map<String, Object> map = JsonUtils.convertToMap(contentModel);

                contentInputBean.setData(map);
                entityInputBean.setContent(contentInputBean);
                TrackResultBean trackResult = mediationFacade.trackEntity(company, entityInputBean);
                model = new Model(trackResult, fortress, documentType);
                contentModelDao.save(model);
            } else {
                updateProfile(company, contentModel, model);
            }
        } catch (ExecutionException | InterruptedException | IOException e) {
            throw new FlockException(e.getMessage());
        }

        return new ContentModelResult(model);
    }

    @Override
    public org.flockdata.profile.model.ContentModel get(Company company, String fortressCode, String documentCode) throws FlockException {
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
    public Collection<ContentModelResult> find(Company company) {
        return contentModelDao.find(company.getId());
    }

    @Override
    @Transactional
    public ContentModelResult find(Company company, String key) throws FlockException {
        ContentModelResult model = contentModelDao.findByKey(company.getId(), key);
        if (model == null) {
            throw new NotFoundException("Unable to locate Content Model from key " + key);
        }
        ContentModel contentModel = getContentModel(company, model.getKey());
        if (contentModel != null) {
            contentModel.setDocumentName(model.getDocumentType());
        }
        model.setContentModel(contentModel);
        return model;
    }

    @Override
    public ContentModel getTagModel(Company company, String code) throws FlockException {
        Model model = contentModelDao.findTagProfile(company, TagHelper.parseKey(code));

        if (model == null)
            throw new NotFoundException(String.format("Unable to locate and tag profile for [%s]", code));

        // Serialized content profile is stored in a log. Here we retrieve the last saved one
        // but we could return the entire history
        ContentModel contentModel = getContentModel(company, model.getKey());
        if (contentModel != null) {
            contentModel.setDocumentName("Tag");
        }
        return contentModel;

    }

    @Override
    @Transactional
    public void delete(Company company, String key) {
        ContentModelResult model = contentModelDao.findByKey(company.getId(), key);
        if ( model !=null )
            contentModelDao.delete(company, model.getKey());

    }

    @Override
    public ContentValidationResults validate(ContentValidationRequest contentRequest) {
        assert contentRequest != null;
        assert contentRequest.getContentModel() != null;
        ContentValidationResults validatedContent = new ContentValidationResults();
        Map<String, ColumnDefinition> content = contentRequest.getContentModel().getContent();
        for (String column : content.keySet()) {
            ContentValidationResult result = new ContentValidationResult(content.get(column), "ok");
            validatedContent.add(result);
        }
        return validatedContent;
    }

    @Override
    public ContentModel createDefaultContentModel(ContentValidationRequest contentRequest) {
        ContentModel result = contentRequest.getContentModel();
        if (result == null)
            result = new ContentModelHandler();

        result.setContent(Transformer.fromMapToModel(contentRequest.getRows()));
        return result;
    }


}
