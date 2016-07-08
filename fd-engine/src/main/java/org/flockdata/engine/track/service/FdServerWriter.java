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

package org.flockdata.engine.track.service;

import org.flockdata.engine.configure.SecurityHelper;
import org.flockdata.geography.service.GeographyService;
import org.flockdata.helper.FlockException;
import org.flockdata.model.Company;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Fortress;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.profile.service.ContentModelService;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.shared.ClientConfiguration;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.service.FortressService;
import org.flockdata.track.service.MediationFacade;
import org.flockdata.transform.FdWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * User: mike
 * Date: 8/10/14
 * Time: 8:47 AM
 */
@Service
public class FdServerWriter implements FdWriter {

    @Autowired
    GeographyService geoService;

    @Autowired
    FortressService fortressService;

    @Autowired
    ConceptService conceptService;

    @Autowired
    MediationFacade mediationFacade;

    @Autowired
    SecurityHelper securityHelper;

    @Autowired
    ContentModelService contentModelService;

    @Override
    public SystemUserResultBean me() {
        return new SystemUserResultBean(securityHelper.getSysUser(true));
    }

    @Override
    public String flushTags(List<TagInputBean> tagInputBeans) throws FlockException {
        return null;
    }

    @Override
    public String flushEntities(Company company, List<EntityInputBean> entityBatch, ClientConfiguration configuration) throws FlockException {
        if ( company == null )
            company = securityHelper.getCompany();
        try {
            for (EntityInputBean entityInputBean : entityBatch) {
                mediationFacade.trackEntity(company, entityInputBean);
            }
            return "ok";
        } catch (InterruptedException e) {
            throw new FlockException("Interrupted", e);
        } catch (ExecutionException e) {
            throw new FlockException("Execution Problem", e);
        } catch (IOException e) {
            throw new FlockException("IO Exception", e);
        }
    }

    @Override
    public ContentModel getContentModel(ClientConfiguration clientConfiguration, String fileModel) throws IOException {
        String[] args = fileModel.split(":");
        // ToDo: this is not yet properly supported - needs to be tested with Tag fortress - which is logical
        if ( args.length == 2){
            Company company = securityHelper.getCompany();
            Fortress fortress = fortressService.getFortress(company, args[0]);
            DocumentType docType = conceptService.findDocumentType(fortress, args[1]);
            try {
                return contentModelService.get(company, fortress, docType);
            } catch (FlockException e) {
                throw new IOException("Problem locating content model ["+fileModel+"]");
            }

        }
        return null;
    }

}
