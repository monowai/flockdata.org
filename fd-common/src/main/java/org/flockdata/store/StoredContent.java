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

package org.flockdata.store;

import org.flockdata.model.Entity;
import org.flockdata.track.bean.ContentInputBean;

import java.io.IOException;
import java.util.Map;

/**
 * User: mike
 * Date: 17/09/14
 * Time: 1:01 PM
 */
public interface StoredContent {

    String getAttachment();

    Map<String, Object> getData() ;

    String getChecksum() throws IOException;

    /**
     *
     * @return primary key for this content
     */
    Object getId();

    ContentInputBean getContent();

    void setStore(String store);

    String getStore();

    String getType();

    Entity getEntity();
}
