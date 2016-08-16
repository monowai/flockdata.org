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
    private static final ObjectMapper mapper = FdJsonObjectMapper.getObjectMapper();

    public static ObjectMapper getMapper() {
        return mapper;
    }
    public static byte[] toJsonBytes(Object object) throws IOException {

        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.writeValueAsBytes(object);
    }

    public static <T> T toObject(byte[] bytes, Class<T> clazz) throws IOException {
        return mapper.readValue(bytes, clazz);
    }

    public static <T> T  toObject(String json, Class<T>  clazz) throws IOException{
        return toObject(json.getBytes(), clazz );
    }

    public static <T> Collection<T> toCollection(String json, Class<T> clazz) throws IOException {
        if (json == null || json.equals(""))
            return new ArrayList<>();

        CollectionType javaType =
                mapper.getTypeFactory().constructCollectionType(List.class, clazz);
        return mapper.readValue(json, javaType );

    }

    /**
     * deserialize the JSON string as a collection of clazz
     *
     * e.g. Collection<EntityInputBean> results = JsonUtils.getAsCollection(message.getBody(), EntityInputBean.class);
     *
     * @param bytes  JSON Bytes - usually String.getBytes()
     * @param clazz  Concrete type
     * @return   Collection<T>
     * @throws IOException
     */
    public static <T> Collection<T> toCollection(byte[] bytes, Class<T> clazz) throws IOException {
        if (bytes == null )
            return new ArrayList<>();

        CollectionType javaType =
                mapper.getTypeFactory().constructCollectionType(List.class, clazz);
        return mapper.readValue(bytes, javaType );

    }

    public static Map<String,Object> toMap(String json) throws IOException {
        Map<String,Object> result = mapper.readValue(json, Map.class);
        return result;
    }

    public static Map<String,Object> toMap(byte[] json) throws IOException {
        Map result = mapper.readValue(json, Map.class);
        return result;
    }

    public static Map convertToMap(Object o) {
        return mapper.convertValue(o, Map.class);
    }

    public static String pretty(Object json)  {
        if(json == null )
            return null;
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String toJson(Object obj) {

        try {
            return new String(toJsonBytes(obj));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
