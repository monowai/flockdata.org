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

package org.flockdata.engine.track.endpoint;

import org.flockdata.configure.SecurityHelper;
import org.flockdata.geography.service.GeographyService;
import org.flockdata.helper.FlockException;
import org.flockdata.model.Company;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityLinkInputBean;
import org.flockdata.track.service.MediationFacade;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FdLoader;
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
    MediationFacade mediationFacade;

    @Autowired
    SecurityHelper securityHelper;

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
    public int flushEntityLinks(List<EntityLinkInputBean> referenceInputBeans) throws FlockException {
        return 0;
    }

    @Override
    public boolean isSimulateOnly() {
        return false;
    }

    @Override
    public void close(FdLoader fdLoader) throws FlockException {
        fdLoader.flush();
    }
}
