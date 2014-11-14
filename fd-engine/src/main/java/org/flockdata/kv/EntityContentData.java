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

package org.flockdata.kv;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.flockdata.helper.CompressionHelper;
import org.flockdata.helper.FlockDataJsonFactory;
import org.flockdata.track.model.EntityContent;
import org.flockdata.track.model.KvContent;
import org.flockdata.track.model.Log;
import org.springframework.data.annotation.Transient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Mike Holdsworth
 * POJO for transmitting Content data to a KV store
 * Since: 4/09/13
 */
public class EntityContentData implements EntityContent {

    private Boolean compressed;

    private KvContent kvContent;

    @Transient
    private
    ObjectMapper objectMapper = FlockDataJsonFactory.getObjectMapper();

    protected EntityContentData() {
    }

    public EntityContentData(byte[] content, Log log) {
        this();
        this.compressed = log.isCompressed();
        this.kvContent = extractBytes(CompressionHelper.decompress(content, compressed));
    }

    @Override
    public Map<String, Object> getWhat() {
        return kvContent.getWhat();
    }

    @Override
    public String getAttachment() {
        return kvContent.getAttachment();
    }

    private KvContent extractBytes(String base64) {
        try {
            KvContent kvContent;
            try {
                kvContent = objectMapper.readValue(base64, KvContentData.class);
            } catch (UnrecognizedPropertyException upe) {
                // Stored as a map
                Map<String, Object> result = objectMapper.readValue(base64, HashMap.class);
                kvContent = new KvContentData(result);
            }
            return kvContent;

        } catch (IOException ignored) {
        }
        return null;
    }




}
