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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: mike
 * Date: 28/10/14
 * Time: 9:30 AM
 */
public class FdJsonObjectMapper extends JsonFactory {
    private static ObjectMapper objectMapper = null;
    private static Lock l = new ReentrantLock();

    /**
     *
     * @return Jackson 2.0 mapper with comments enabled
     */
    public static ObjectMapper getObjectMapper(){
        if ( objectMapper == null ){
            try {
                l.lock();
                if ( objectMapper == null ){
                    objectMapper = new ObjectMapper( new FdJsonObjectMapper());
                    //objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                }

            } finally {
                l.unlock();
            }
        }

        return objectMapper;
    }

    @Override
    public JsonParser createParser(URL url) throws IOException {
        JsonParser p = super.createParser(url);
        setParserFeatures(p);
        return p;
    }

    private void setParserFeatures(JsonParser p) {
        p.enable(JsonParser.Feature.ALLOW_COMMENTS);
    }

    @Override
    public JsonParser createParser(InputStream stream) throws IOException {
        JsonParser p = super.createParser(stream);
        setParserFeatures(p);
        return p;
    }

    @Override
    public JsonParser createParser(File file) throws IOException {
        JsonParser p = super.createParser(file);
        setParserFeatures(p);
        return p;
    }
}
