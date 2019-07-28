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
import java.util.concurrent.ExecutionException;
import org.flockdata.authentication.SecurityHelper;
import org.flockdata.data.Company;
import org.flockdata.data.ContentModel;
import org.flockdata.data.Document;
import org.flockdata.data.Fortress;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.tag.MediationFacade;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.services.ContentModelService;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.FdIoInterface;
import org.flockdata.transform.model.ExtractProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Server side IO writer that uses injected services rather than HTTP/AMQP communication mechanisms
 * Used to support server side processing of data files
 *
 * @author mholdsworth
 * @tag Batch, Entity, Tag
 * @since 8/10/2014
 */
@Service
public class FdServerIo implements FdIoInterface {

  private final FortressService fortressService;

  private final ConceptService conceptService;

  private final MediationFacade mediationFacade;

  private final SecurityHelper securityHelper;

  private final ContentModelService contentModelService;

  @Autowired
  public FdServerIo(MediationFacade mediationFacade, SecurityHelper securityHelper, ContentModelService contentModelService, FortressService fortressService, ConceptService conceptService) {
    this.mediationFacade = mediationFacade;
    this.securityHelper = securityHelper;
    this.contentModelService = contentModelService;
    this.fortressService = fortressService;
    this.conceptService = conceptService;
  }

  @Override
  public SystemUserResultBean me() {
    return new SystemUserResultBean(securityHelper.getSysUser(false));
  }

  @Override
  public String writeTags(Collection<TagInputBean> tagInputBeans) throws FlockException {
    Company company = securityHelper.getCompany();
    try {
      mediationFacade.createTags(company, tagInputBeans);
    } catch (ExecutionException | InterruptedException e) {
      throw new FlockException("Interrupted", e);
    }
    return null;
  }

  @Override
  public String writeEntities(Collection<EntityInputBean> entityBatch) throws FlockException {
    Company company = securityHelper.getCompany();
    try {
      for (EntityInputBean entityInputBean : entityBatch) {
        mediationFacade.trackEntity(company, entityInputBean);
      }
      return "ok";
    } catch (InterruptedException e) {
      throw new FlockException("Interrupted", e);
    } catch (ExecutionException e) {
      throw new FlockException("Execution Problem", e);
    }
  }

  @Override
  public ContentModel getContentModel(Fortress fortress, Document documentType) {
    return null;
  }


  @Override
  public ContentModel getContentModel(String modelKey) throws IOException {
    String[] args = modelKey.split(":");
    // ToDo: this is not yet properly supported - needs to be tested with Tag fortress - which is logical
    if (args.length == 2) {
      Company company = securityHelper.getCompany();
      Fortress fortress = fortressService.getFortress(company, args[0]);
      DocumentNode docType = conceptService.findDocumentType(fortress, args[1]);
      try {
        return contentModelService.get(company, fortress, docType);
      } catch (FlockException e) {
        throw new IOException("Problem locating content model [" + modelKey + "]");
      }

    }
    return null;
  }

  @Override
  public SystemUserResultBean validateConnectivity() throws FlockException {
    return me();
  }

  @Override
  public SystemUserResultBean login(String userName, String password) {
    throw new UnsupportedOperationException("login is not supported in this class");
  }

  @Override
  public String getUrl() {
    throw new UnsupportedOperationException("This function is not supported");
  }

  @Override
  public RestTemplate getRestTemplate() {
    return null;
  }

  @Override
  public HttpHeaders getHeaders() {
    return null;
  }

  @Override
  public ExtractProfile getExtractProfile(String fileModel, ContentModel contentModel) {
    // ToDo: FixMe
    return null;
  }

}
