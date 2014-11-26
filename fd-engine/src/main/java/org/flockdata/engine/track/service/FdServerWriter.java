/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.engine.track.service;

import org.flockdata.geography.service.GeographyService;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.SecurityHelper;
import org.flockdata.registration.bean.SystemUserResultBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Tag;
import org.flockdata.track.bean.CrossReferenceInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.service.MediationFacade;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FdReader;
import org.flockdata.transform.FdWriter;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * User: mike
 * Date: 8/10/14
 * Time: 8:47 AM
 */
@Service
public class FdServerWriter implements FdWriter, FdReader {

    @Autowired
    GeographyService geoService;

    @Autowired
    MediationFacade mediationFacade;

    @Autowired
    SecurityHelper securityHelper;

    // ToDo: Yes this is useless - fix me!
    final String lock = "countryLock";
    private Map<String, Tag> countries = new HashMap<>();

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(FdServerWriter.class);

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
    public int flushXReferences(List<CrossReferenceInputBean> referenceInputBeans) throws FlockException {
        return 0;
    }

    @Override
    public boolean isSimulateOnly() {
        return false;
    }

    @Override
    public Collection<Tag> getCountries() throws FlockException {

        return geoService.findCountries(securityHelper.getCompany());
    }

    @Override
    public String resolveCountryISOFromName(String name) throws FlockException {
        // 2 char country? it's already ISO
        if (name.length() == 2)
            return name;

        if (countries.isEmpty()) {
            synchronized (lock) {
                if (countries.isEmpty()) {
                    Collection<Tag> results = getCountries();

                    for (Tag next : results) {
                        countries.put(next.getName().toLowerCase(), next);
                    }
                }
            }
        }
        Tag tag = countries.get(name.toLowerCase());
        if (tag == null) {
            logger.error("Unable to resolve country name [{}]", name);
            return null;
        }
        return tag.getCode();

    }

    @Override
    public String resolve(String type, Map<String, Object> args) {
        return null;
    }
}
