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

package org.flockdata.test.client;

import org.flockdata.helper.FlockException;
import org.flockdata.registration.bean.SystemUserResultBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.model.Company;
import org.flockdata.track.bean.CrossReferenceInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FdWriter;
import org.flockdata.transform.TrackBatcher;

import java.util.List;

/**
 * Created by mike on 1/03/15.
 */
public class MockFdWriter implements FdWriter {

    public List<EntityInputBean> getEntities() {
        return entities;
    }

    public List<TagInputBean> getTags() {
        return tags;
    }

    public List<EntityInputBean> entities = null;
    public List<TagInputBean> tags = null;


    @Override
    public SystemUserResultBean me() {
        return null;
    }

    @Override
    public String flushTags(List<TagInputBean> tagInputBeans) throws FlockException {
        this.tags = tagInputBeans;
        return null;
    }

    @Override
    public String flushEntities(Company company, List<EntityInputBean> entityBatch, ClientConfiguration configuration) throws FlockException {
        this.entities = entityBatch;
        return null;
    }

    @Override
    public int flushXReferences(List<CrossReferenceInputBean> referenceInputBeans) throws FlockException {
        return 0;
    }
    private boolean simulateOnly = false;
    @Override
    public boolean isSimulateOnly() {
        // Setting this to true will mean that the flush routines above are not called
        return simulateOnly;
    }

    @Override
    public void close(TrackBatcher trackBatcher) throws FlockException {
        this.entities = trackBatcher.getEntities();
        this.tags = trackBatcher.getTags();
//        simulateOnly = true;
//        trackBatcher.flush();
    }
}
