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

package org.flockdata.transform;

import org.flockdata.helper.FlockException;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;

import java.util.List;

/**
 * Created by mike on 5/03/16.
 */
public interface PayloadBatcher {
    void batchTag(TagInputBean tagInputBean, String message) throws FlockException;

    void batchEntity(EntityInputBean entityInputBean) throws FlockException;

    void batchEntity(EntityInputBean entityInputBean, boolean flush) throws FlockException;

    void flush();

    void reset();

    List<EntityInputBean> getEntities();

    List<TagInputBean> getTags();

    ContentModel getContentModel(String model);
}
