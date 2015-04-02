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

package org.flockdata.helper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * User: mike
 * Date: 28/08/14
 * Time: 3:38 PM
 */
public class JsonUtils {
    private static final ObjectMapper mapper = FlockDataJsonFactory.getObjectMapper();
    public static byte[] getObjectAsJsonBytes(Object object) throws IOException {

        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.writeValueAsBytes(object);
    }

    public static <T> T getBytesAsObject (byte[] bytes, Class<T> clazz) throws IOException {
        return mapper.readValue(bytes, clazz);
    }


    public static <T> Collection<T> getAsCollection(String json, Class<T> clazz) throws IOException {
        if (json == null || json.equals(""))
            return new ArrayList<>();

        CollectionType javaType =
                mapper.getTypeFactory().constructCollectionType(List.class, clazz);
        return mapper.readValue(json, javaType );

    }

    public static <T> Collection<T> getAsCollection(byte[] bytes, Class<T> clazz) throws IOException {
        if (bytes == null )
            return new ArrayList<>();

        CollectionType javaType =
                mapper.getTypeFactory().constructCollectionType(List.class, clazz);
        return mapper.readValue(bytes, javaType );

    }

    public static Map<String,Object> getAsMap(String json) throws IOException {
        Map<String,Object> result = mapper.readValue(json, Map.class);
        return result;
    }

    public static String getJSON(Object obj) {
    	String json = null;
    	try {
			json = mapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
    	return json;
    }
}
