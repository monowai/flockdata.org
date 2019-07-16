/*
 *  Copyright 2012-2017 the original author or authors.
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

package org.flockdata.test.unit.client;

import java.io.IOException;
import java.util.Collection;
import org.flockdata.data.ContentModel;
import org.flockdata.data.Document;
import org.flockdata.data.Fortress;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.FdIoInterface;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.model.ExtractProfile;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Mock writer that satisfies the interface for testing purposes
 *
 * @author mholdsworth
 * @since 1/03/2015
 */
@Service
@Profile( {"dev"})
public class FdMockIo implements FdIoInterface {

    public Collection<EntityInputBean> entities = null;
    public Collection<TagInputBean> tags = null;

    /**
     * @return entities that would have been written to the service
     */
    public Collection<EntityInputBean> getEntities() {
        return entities;
    }

    /**
     * @return tags that would have been written to the service
     */
    public Collection<TagInputBean> getTags() {
        return tags;
    }

    @Override
    public SystemUserResultBean me() {
        return null;
    }

    /**
     * @param tagInputBeans tags to write
     * @return usually an error message, but null for testing purposes
     * @throws FlockException should never happen
     */
    @Override
    public String writeTags(Collection<TagInputBean> tagInputBeans) throws FlockException {
        this.tags = tagInputBeans;
        return null;
    }

    @Override
    public String writeEntities(Collection<EntityInputBean> entityBatch) throws FlockException {
        this.entities = entityBatch;
        return null;
    }

    @Override
    public ContentModel getContentModel(Fortress fortress, Document documentType) {
        //return getContentModel(fortress.getName(), documentType.getCode());
        return null;
    }

    @Override
    public ContentModel getContentModel(String modelKey) throws IOException {
        return ContentModelDeserializer.getContentModel(modelKey);
    }

    @Override
    public SystemUserResultBean validateConnectivity() throws FlockException {
        return null;
        // noop
    }

    @Override
    public SystemUserResultBean login(String userName, String password) {
        return me();
    }

    @Override
    public String getUrl() {
        return "mock";
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
        return null;
    }
}
