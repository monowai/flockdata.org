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

package org.flockdata.engine.model.endpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.flockdata.data.ContentModel;
import org.flockdata.data.Document;
import org.flockdata.data.Fortress;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.query.service.ContentService;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.engine.track.service.FortressService;
import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.ContentModelResult;
import org.flockdata.model.ContentValidationRequest;
import org.flockdata.model.ContentValidationResults;
import org.flockdata.search.ContentStructure;
import org.flockdata.services.ContentModelService;
import org.flockdata.transform.DataConversionRequest;
import org.flockdata.transform.Transformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author mholdsworth
 * @tag Endpoint, ContentModel
 * @since 8/11/2013
 */
@RestController
@RequestMapping("${org.fd.engine.system.api:api}/v1/model")
public class ModelEP {

  private final ContentModelService contentModelService;
  private final FortressService fortressService;
  private final ConceptService conceptService;
  private final ContentService contentService;


  @Autowired
  public ModelEP(ContentService contentService, ContentModelService contentModelService, FortressService fortressService, ConceptService conceptService) {
    this.contentModelService = contentModelService;
    this.fortressService = fortressService;
    this.conceptService = conceptService;
    this.contentService = contentService;
  }

  @RequestMapping(value = "/",
      produces = "application/json",
      method = RequestMethod.GET)
  public Collection<ContentModelResult> getModels(HttpServletRequest request) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    return contentModelService.find(company);
  }

  @RequestMapping(value = "/",
      produces = "application/json",
      consumes = "application/json",
      method = RequestMethod.POST)
  public Collection<ContentModelResult> storeModels(@RequestBody Collection<ContentModel> contentModels, HttpServletRequest request) throws FlockException {

    CompanyNode company = CompanyResolver.resolveCompany(request);
    Collection<ContentModelResult> results = new ArrayList<>();
    for (ContentModel contentModel : contentModels) {
      ContentModelResult result;
      if (contentModel.isTagModel()) {
        result = saveContentModel(request, contentModel.getCode(), contentModel);
      } else {
        FortressNode fortress = fortressService.registerFortress(company, contentModel.getFortress());
        conceptService.save(new DocumentNode(fortress.getDefaultSegment(), contentModel));
        result = saveContentModel(request, contentModel.getFortress().getName(), contentModel.getDocumentType().getName(), contentModel);
      }
      results.add(result);
    }
    return results;
  }

  @RequestMapping(value = "/download",
      produces = "application/json",
      consumes = "application/json",
      method = RequestMethod.POST)
  public Collection<ContentModel> getModels(@RequestBody Collection<String> modelKeys, HttpServletRequest request) throws FlockException {

    CompanyNode company = CompanyResolver.resolveCompany(request);
    Collection<ContentModel> results = new ArrayList<>();
    for (String modelKey : modelKeys) {
      ContentModelResult cmr = contentModelService.find(company, modelKey);
      if (cmr != null) {
        results.add(cmr.getContentModel());
      }
    }
    return results;
  }


  @RequestMapping(value = "/{key}",
      produces = "application/json",
      method = RequestMethod.DELETE)
  public void deleteModelKey(HttpServletRequest request, @PathVariable("key") String key) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    contentModelService.delete(company, key);
  }

  @RequestMapping(value = "/{key}",
      produces = "application/json",
      method = RequestMethod.GET)
  public ContentModelResult getContentModelByKey(HttpServletRequest request, @PathVariable("key") String key) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    return contentModelService.find(company, key);
  }


  @RequestMapping(value = "/{fortressCode}/{docTypeName}",
      produces = "application/json", method = RequestMethod.GET)
  @ResponseBody
  public ContentModel getContentModel(
      HttpServletRequest request,
      @PathVariable("fortressCode") String fortressCode,
      @PathVariable("docTypeName") String docTypeName) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);

    Fortress fortress = fortressService.getFortress(company, fortressCode);
    if (fortress == null) {
      throw new IllegalArgumentException("Unable to locate the fortress " + fortressCode);
    }

    Document documentType = conceptService.resolveByDocCode(fortress, docTypeName, Boolean.FALSE);
    if (documentType == null) {
      throw new IllegalArgumentException("Unable to locate the document " + docTypeName);
    }

    return contentModelService.get(company, fortress, documentType);

  }

  @RequestMapping(value = "/tag/{code}",
      produces = "application/json", method = RequestMethod.GET)
  @ResponseBody
  public ContentModel getContentModel(
      HttpServletRequest request,
      @PathVariable("code") String code) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);


    return contentModelService.getTagModel(company, code);

  }

  @RequestMapping(value = "/{fortressCode}/{docTypeName}",
      produces = "application/json",
      consumes = "application/json",
      method = RequestMethod.POST)
  public ContentModelResult saveContentModel(HttpServletRequest request,
                                             @PathVariable("fortressCode") String fortressCode,
                                             @PathVariable("docTypeName") String docTypeName,
                                             @RequestBody ContentModel contentModel) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);

    FortressNode fortress = fortressService.registerFortress(company, contentModel.getFortress(), false);
    if (fortress == null && contentModel.getFortress() != null) {
      fortress = fortressService.registerFortress(company, contentModel.getFortress(), true);
    }

    if (fortress == null) {
      throw new IllegalArgumentException("Unable to locate the fortress " + fortressCode);
    }

    DocumentNode documentType = conceptService.resolveByDocCode(fortress, docTypeName, false);

    if (documentType == null && contentModel.getDocumentType() != null) {
      documentType = conceptService.findOrCreate(fortress, new DocumentNode(fortress, contentModel));
    }

    if (documentType == null) {
      throw new IllegalArgumentException("Unable to locate the document " + docTypeName);
    }

    return contentModelService.saveEntityModel(company, fortress, documentType, contentModel);

  }

  @RequestMapping(value = "/{fortress}/{documentType}/fields", produces = "application/json", method = RequestMethod.GET)
  public
  @ResponseBody
  ContentStructure getEntityFields(@PathVariable("fortress") String fortressName,
                                   @PathVariable("documentType") String documentType,
                                   HttpServletRequest request) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    FortressNode fortress = fortressService.findByCode(company, fortressName);
    if (fortress == null) {
      throw new NotFoundException("Unable to locate fortress " + fortressName);
    }


    return contentService.getStructure(company, fortress, documentType);
  }

  @RequestMapping(value = "/tag/{code}",
      produces = "application/json",
      consumes = "application/json",
      method = RequestMethod.POST)
  public ContentModelResult saveContentModel(HttpServletRequest request,
                                             @PathVariable("code") String code,
                                             @RequestBody ContentModel contentModel) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);

    if (code == null || code.equals("")) {
      throw new IllegalArgumentException("No key code was provided for the model");
    }


    return contentModelService.saveTagModel(company, code, contentModel);

  }


  @RequestMapping(value = "/validate",
      produces = "application/json",
      consumes = "application/json",
      method = RequestMethod.POST)
  public ContentValidationResults validateContent(HttpServletRequest request,
                                                  @RequestBody ContentValidationRequest contentRequest) throws FlockException {
    CompanyResolver.resolveCompany(request);

    return contentModelService.validate(contentRequest);

  }

  @RequestMapping(value = "/default",
      produces = "application/json",
      consumes = "application/json",
      method = RequestMethod.POST)
  public ContentModel defaultContentModel(HttpServletRequest request,
                                          @RequestBody ContentValidationRequest contentRequest) {
    CompanyResolver.resolveCompany(request);
    return contentModelService.createDefaultContentModel(contentRequest);
  }


  @RequestMapping(value = "/map",
      produces = "application/json",
      consumes = "application/json",
      method = RequestMethod.POST)
  public Collection<Map<String, Object>> dataMap(HttpServletRequest request,
                                                 @RequestBody DataConversionRequest conversionRequest)
      throws FlockException {
    CompanyResolver.resolveCompany(request);
    return Transformer.convertToMap(conversionRequest);
  }


}
