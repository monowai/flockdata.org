/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.test.client;

import org.flockdata.helper.FlockException;
import org.flockdata.model.Company;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.shared.ClientConfiguration;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.FdWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Mock writer that satisfies the interface for testing purposes
 *
 * Created by mike on 1/03/15.
 */
@Service
@Profile("dev")
public class MockFdWriter implements FdWriter {

    public List<EntityInputBean> getEntities() {
        return entities;
    }

    public List<TagInputBean> getTags() {
        return tags;
    }

    public List<EntityInputBean> entities = null;
    public List<TagInputBean> tags = null;

    @Autowired
    ClientConfiguration clientConfiguration;

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

}
