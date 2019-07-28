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

package org.flockdata.engine.model.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.transaction.Transactional;
import org.flockdata.authentication.SecurityHelper;
import org.flockdata.data.Company;
import org.flockdata.data.ContentModel;
import org.flockdata.data.Document;
import org.flockdata.data.Fortress;
import org.flockdata.data.Model;
import org.flockdata.engine.data.dao.ContentModelDaoNeo;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.data.graph.ModelNode;
import org.flockdata.engine.tag.MediationFacade;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.engine.track.service.EntityService;
import org.flockdata.engine.track.service.FortressService;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.JsonUtils;
import org.flockdata.helper.NotFoundException;
import org.flockdata.helper.TagHelper;
import org.flockdata.model.ColumnValidationResult;
import org.flockdata.model.ContentModelResult;
import org.flockdata.model.ContentValidationRequest;
import org.flockdata.model.ContentValidationResults;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.services.ContentModelService;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.ExpressionHelper;
import org.flockdata.transform.Transformer;
import org.flockdata.transform.model.ContentModelHandler;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * @author mholdsworth
 * @tag Service, ContentModel
 * @since 3/10/2014
 */
@Service
@Transactional
public class ContentModelServiceNeo implements ContentModelService {

  private static final ObjectMapper objectMapper = new ObjectMapper(new FdJsonObjectMapper())
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .enable(JsonParser.Feature.ALLOW_COMMENTS);
  private final ContentModelDaoNeo contentModelDao;
  private final FortressService fortressService;
  private final EntityService entityService;
  private final ConceptService conceptService;
  private final MediationFacade mediationFacade;
  private final SecurityHelper securityHelper;

  @Autowired
  public ContentModelServiceNeo(MediationFacade mediationFacade, ContentModelDaoNeo contentModelDao, FortressService fortressService, SecurityHelper securityHelper, ConceptService conceptService, EntityService entityService) {
    this.mediationFacade = mediationFacade;
    this.contentModelDao = contentModelDao;
    this.fortressService = fortressService;
    this.securityHelper = securityHelper;
    this.conceptService = conceptService;
    this.entityService = entityService;
  }

  public ContentModel get(Company company, Fortress fortress, Document documentType) throws FlockException {
    Model model = contentModelDao.find(fortress, documentType);

    if (model == null) {
      throw new NotFoundException(String.format("Unable to locate and import model for [%s], [%s]", fortress.getCode(), documentType.getName()));
    }

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

  public ContentModelResult saveTagModel(Company company, String code, ContentModel contentModel) throws FlockException {
    FortressNode internalFortress = fortressService.findInternalFortress(company);

    assert internalFortress != null;

    String profileCode = TagHelper.parseKey(code);
    contentModel.setTagModel(true);

    ModelNode existingModel = contentModelDao.findTagProfile(company, profileCode);
    try {
      if (existingModel == null) {
        EntityInputBean entityInputBean = new EntityInputBean(internalFortress, new DocumentTypeInputBean("TagModel"));
        entityInputBean.setCode(code);
        entityInputBean.setName(contentModel.getName());
        ContentInputBean contentInputBean = new ContentInputBean(securityHelper.getLoggedInUser(), new DateTime());
        Map<String, Object> map = JsonUtils.convertToMap(contentModel);

        contentInputBean.setData(map);
        entityInputBean.setContent(contentInputBean);
        TrackResultBean trackResult = mediationFacade.trackEntity(company, entityInputBean);
        Document documentType = conceptService.findDocumentType(internalFortress, "TagModel");
        existingModel = new ModelNode(company, trackResult, profileCode, documentType);
        contentModelDao.save(existingModel);
      } else {
        updateProfile(company, contentModel.setTagModel(true), existingModel);
      }
    } catch (ExecutionException | InterruptedException | IOException e) {
      throw new FlockException(e.getMessage());
    }

    return new ContentModelResult(existingModel);

  }

  private void updateProfile(Company company, ContentModel contentModelConfig, ModelNode existingModel) throws FlockException, IOException, ExecutionException, InterruptedException {
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
   * @param company      owner of the model
   * @param fortress     system the model belongs to
   * @param documentType type of document
   * @param contentModel data
   * @return result of execution
   * @throws FlockException error
   */
  @Transactional
  public ContentModelResult saveEntityModel(Company company, Fortress fortress, Document documentType, ContentModel contentModel) throws FlockException {
    // Used for storing internal versionable data
    FortressNode internalFortress = fortressService.findInternalFortress(company);
    assert internalFortress != null;

    ModelNode model = contentModelDao.find(fortress, documentType);
    try {
      if (model == null) {
        EntityInputBean entityInputBean = new EntityInputBean(internalFortress, new DocumentTypeInputBean("FdContentModel"));
        entityInputBean.setName(contentModel.getName());
        ContentInputBean contentInputBean = new ContentInputBean(securityHelper.getLoggedInUser(), new DateTime());
        Map<String, Object> map = JsonUtils.convertToMap(contentModel);

        contentInputBean.setData(map);
        entityInputBean.setContent(contentInputBean);
        TrackResultBean trackResult = mediationFacade.trackEntity(company, entityInputBean);
        model = new ModelNode(company, trackResult, fortress, documentType);
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
  public ContentModel get(Company company, String fortressCode, String documentCode) throws FlockException {
    FortressNode fortress = fortressService.findByCode(company, fortressCode);
    if (fortress == null) {
      throw new NotFoundException("Unable to locate the fortress " + fortressCode);
    }
    DocumentNode documentType = conceptService.resolveByDocCode(fortress, documentCode, false);
    if (documentType == null) {
      throw new NotFoundException("Unable to resolve document type " + documentCode);
    }

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

    if (model == null) {
      throw new NotFoundException(String.format("Unable to locate and tag profile for [%s]", code));
    }

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
  @Retryable
  public void delete(Company company, String key) {
    ContentModelResult model = contentModelDao.findByKey(company.getId(), key);
    if (model != null) {
      contentModelDao.delete(company, model.getKey());
    }

  }

  @Override
  public ContentValidationResults validate(ContentValidationRequest contentRequest) {
    assert contentRequest != null;
    assert contentRequest.getContentModel() != null;
    ContentValidationResults validatedContent = new ContentValidationResults();

    if (contentRequest.getRows() == null) {
      return validatedContent;
    }

    int row = 0;
    if (contentRequest.getContentModel().isTagModel()) {

      for (Map<String, Object> dataRow : contentRequest.getRows()) {
        validatedContent.addResults(row, validate(dataRow, contentRequest.getContentModel().getContent()));
        try {
          validatedContent.add(row, Transformer.toTags(dataRow, contentRequest.getContentModel()));
        } catch (Exception e) {
          validatedContent.addMessage(row, e.getMessage());
        }
        row++;
      }

    } else {// do entity
      for (Map<String, Object> dataRow : contentRequest.getRows()) {
        validatedContent.addResults(row, validate(dataRow, contentRequest.getContentModel().getContent()));
        try {
          validatedContent.add(row, Transformer.toEntity(dataRow, contentRequest.getContentModel()));
        } catch (Exception e) {
          validatedContent.addMessage(row, e.getMessage());
        }
        row++;
      }


    }
    return validatedContent;
  }

  /**
   * Validate the data against the expressions in the ContentModel
   *
   * @param row                 data to validate
   * @param columnDefinitionMap validate against this
   * @return Identified validation errors
   */
  private Collection<ColumnValidationResult> validate(Map<String, Object> row, Map<String, ColumnDefinition> columnDefinitionMap) {
    Collection<ColumnValidationResult> results = new ArrayList<>();

    for (String source : columnDefinitionMap.keySet()) {
      Collection<String> messages = new ArrayList<>();
      ColumnDefinition column = columnDefinitionMap.get(source);
      String sourceColumn = column.getSource();
      try {
        if (sourceColumn == null) {
          sourceColumn = source;
        }

        Object oVal = row.get(sourceColumn);

        Object o = ExpressionHelper.getValue(row, column.getValue(), column, oVal);
        if (o == null && column.getValue() != null) {
          messages.add("Null was calculated for express [" + column.getValue() + "]");
        }
        if (!Transformer.isValidForEs(sourceColumn)) {
          messages.add(sourceColumn + " is not valid for ElasticSearch");
        }
      } catch (Exception e) {
        messages.add(e.getMessage());
      }
      results.add(new ColumnValidationResult(source, columnDefinitionMap.get(source), messages).setExpression(column.getValue()));

    }
    return results;
  }

  @Override
  public ContentModel createDefaultContentModel(ContentValidationRequest contentRequest) {
    ContentModel result = contentRequest.getContentModel();
    if (result == null) {
      result = new ContentModelHandler();
    }

    result.setContent(Transformer.fromMapToModel(contentRequest.getRows()));
    return result;
  }


}
