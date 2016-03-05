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

package org.flockdata.helper;

import com.fasterxml.jackson.annotation.JsonInclude;
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


    public static String toJson(Object obj) {

        try {
            return new String(toJsonBytes(obj));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
