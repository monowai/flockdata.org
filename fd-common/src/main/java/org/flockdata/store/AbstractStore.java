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

package org.flockdata.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.flockdata.store.bean.StorageBean;
import org.flockdata.track.bean.ContentInputBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mholdsworth
 * @since 7/03/2015
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
    if (oResult == null) {
      return null;
    }
    if (oResult instanceof ContentInputBean) {
      return new StorageBean(key, (ContentInputBean) oResult);
    } else if (oResult instanceof Map) {
      return new StorageBean(key, (Map<String, Object>) oResult);
    } else {
      logger.error("Unable to handle object result " + oResult.getClass().getCanonicalName());
      return null;
    }

  }


}
