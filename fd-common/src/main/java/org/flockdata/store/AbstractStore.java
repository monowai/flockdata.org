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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.flockdata.store.bean.StorageBean;
import org.flockdata.track.bean.ContentInputBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mike on 7/03/15.
 */
public abstract class AbstractStore implements FdStoreRepo {

    private static Logger logger = LoggerFactory.getLogger(AbstractStore.class);

    ObjectMapper objectMapper = new ObjectMapper();

    public ContentInputBean extractBytes(String base64Json) throws IOException {
        try {
            return objectMapper.readValue(base64Json, ContentInputBean.class);
        } catch (UnrecognizedPropertyException upe) {
            // Stored as a map
            Map<String, Object> result = objectMapper.readValue(base64Json, HashMap.class);
            return new StorageBean(result).getContent();
        }

    }

    protected StoredContent getContent(Object key, Object oResult) {
        if ( oResult == null )
            return null;
        if (oResult instanceof ContentInputBean)
            return new StorageBean(key, (ContentInputBean)oResult );
        else if ( oResult instanceof Map)
            return new StorageBean(key, (Map<String, Object>) oResult);
        else {
            logger.error("Unable to handle object result " + oResult.getClass().getCanonicalName());
            return null;
        }

    }


}
