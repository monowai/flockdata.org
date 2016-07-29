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
import org.flockdata.helper.FlockException;
import org.flockdata.model.Company;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Fortress;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.profile.service.ContentModelService;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.service.FortressService;
import org.flockdata.track.service.MediationFacade;
import org.flockdata.transform.FdIoInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

/**
 * User: mike
 * Date: 8/10/14
 * Time: 8:47 AM
 */
@Service
public class FdServerWriter implements FdIoInterface {

    private final FortressService fortressService;

    private final ConceptService conceptService;

    private final MediationFacade mediationFacade;

    private final SecurityHelper securityHelper;

    private final ContentModelService contentModelService;

    @Autowired
    public FdServerWriter(MediationFacade mediationFacade, SecurityHelper securityHelper, ContentModelService contentModelService, FortressService fortressService, ConceptService conceptService) {
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
        } catch (IOException e) {
            throw new FlockException("IO Exception", e);
        }
    }


    @Override
    public ContentModel getContentModel(String modelKey) throws IOException {
        String[] args = modelKey.split(":");
        // ToDo: this is not yet properly supported - needs to be tested with Tag fortress - which is logical
        if ( args.length == 2){
            Company company = securityHelper.getCompany();
            Fortress fortress = fortressService.getFortress(company, args[0]);
            DocumentType docType = conceptService.findDocumentType(fortress, args[1]);
            try {
                return contentModelService.get(company, fortress, docType);
            } catch (FlockException e) {
                throw new IOException("Problem locating content model ["+ modelKey +"]");
            }

        }
        return null;
    }

    @Override
    public SystemUserResultBean validateConnectivity() throws FlockException {
        return null;
        // No-op - We're on the server, so we better be running
    }

    @Override
    public SystemUserResultBean register(String userName, String company) {
        throw new UnsupportedOperationException("Registration is not supported through this mechanism");
    }

}
