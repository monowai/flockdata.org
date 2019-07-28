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

package org.flockdata.engine.track.service;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import org.flockdata.data.Company;
import org.flockdata.data.ContentModel;
import org.flockdata.data.Document;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.tag.service.TagService;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.integration.FileProcessor;
import org.flockdata.model.ContentValidationRequest;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.services.ContentModelService;
import org.flockdata.track.bean.FdTagResultBean;
import org.flockdata.transform.model.ExtractProfileHandler;
import org.flockdata.transform.tag.TagPayloadTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @author mholdsworth
 * @since 24/05/2016
 */
@Service
class BatchServiceRunner implements BatchService {

  private final
  FortressService fortressService;

  private final
  ConceptService conceptService;

  private final
  ContentModelService profileService;

  private final
  FileProcessor fileProcessor;

  private final
  TagService tagService;

  @Autowired
  public BatchServiceRunner(ContentModelService profileService, ConceptService conceptService, TagService tagService, FortressService fortressService, FileProcessor fileProcessor) {
    this.profileService = profileService;
    this.conceptService = conceptService;
    this.tagService = tagService;
    this.fortressService = fortressService;
    this.fileProcessor = fileProcessor;
  }

  /**
   * Does not validate the arguments.
   */
  @Override
  @Async("fd-track")
  public void processAsync(CompanyNode company, String fortressCode, String documentCode, String file) throws ClassNotFoundException, FlockException, InstantiationException, IOException, IllegalAccessException {
    process(company, fortressCode, documentCode, file, true);
  }

  @Override
  public void process(CompanyNode company, String fortressCode, String documentCode, String file, boolean async) throws FlockException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
    FortressNode fortress = fortressService.findByCode(company, fortressCode);
    DocumentNode documentType = conceptService.resolveByDocCode(fortress, documentCode, false);
    if (documentType == null) {
      throw new NotFoundException("Unable to resolve document type ");
    }
    process(company, fortress, documentType, file, async);
  }

  public int process(Company company, FortressNode fortress, Document documentType, String file, Boolean async) throws FlockException, ClassNotFoundException, IOException, InstantiationException, IllegalAccessException {
    ContentModel profile = profileService.get(company, fortress, documentType);
    // Users PUT params override those of the contentProfile
    if (!profile.getFortress().getName().equalsIgnoreCase(fortress.getName())) {
      profile.setFortress(new FortressInputBean(fortress.getName(), !fortress.isSearchEnabled()));
    }
    if (!profile.getDocumentType().getName().equalsIgnoreCase(documentType.getName())) {
      profile.setDocumentName(documentType.getName());
    }
    FileProcessor.validateArgs(file);
    return fileProcessor.processFile(new ExtractProfileHandler(profile), file);
  }

  @Override
  public void validateArguments(CompanyNode company, String fortressCode, String documentCode, String fileName) throws NotFoundException, IOException {
    if (!FileProcessor.validateArgs(fileName)) {
      throw new NotFoundException("Unable to process filename " + fileName);
    }
    FortressNode fortress = fortressService.findByCode(company, fortressCode);
    if (fortress == null) {
      throw new NotFoundException("Unable to locate the fortress " + fortressCode);
    }
    DocumentNode documentType = conceptService.resolveByDocCode(fortress, documentCode, false);
    if (documentType == null) {
      throw new NotFoundException("Unable to resolve document type " + documentCode);
    }


  }

  @Override
  public ContentValidationRequest process(CompanyNode company, ContentValidationRequest validationRequest) {
    if (validationRequest.getContentModel().isTagModel()) {
      return trackTags(company, validationRequest);
    }

    return null;
  }

  private ContentValidationRequest trackTags(CompanyNode company, ContentValidationRequest validationRequest) {
    int rowCount = 0;
    TagPayloadTransformer tagTransformer = TagPayloadTransformer.newInstance(validationRequest.getContentModel());
    for (Map<String, Object> row : validationRequest.getRows()) {
      try {
        tagTransformer.transform(row);
        Collection<FdTagResultBean> tagResultBeans = tagService.createTags(company, tagTransformer.getTags());
        for (TagResultBean result : tagResultBeans) {
          if (result.isNewTag()) {
            validationRequest.addResult(rowCount, "Previously Unknown");
          } else {
            validationRequest.addResult(rowCount, "Known");
          }
        }
      } catch (FlockException e) {
        validationRequest.addResult(rowCount, e.getMessage());
        // log an error
      }
      rowCount++;
    }
    return validationRequest;
  }
}
