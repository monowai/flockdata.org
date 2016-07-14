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

package org.flockdata.profile;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.profile.model.ExtractProfile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * User: mike
 * Date: 9/05/14
 * Time: 8:45 AM
 */
public class ExtractProfileDeserializer extends JsonDeserializer<ExtractProfile> {

    @Override
    public ExtractProfile deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ExtractProfileHandler importProfile = new ExtractProfileHandler();
        JsonNode node = jp.getCodec().readTree(jp);
        JsonNode nodeValue;

        // ********
        // Batch handling
        // ********
        nodeValue = node.get("header");
        if (!isNull(nodeValue))
            importProfile.setHeader(Boolean.parseBoolean(nodeValue.asText()));

        nodeValue = node.get("handler");
        if (!isNull(nodeValue))
            importProfile.setHandler(nodeValue.asText());

        nodeValue = node.get("preParseRowExp");
        if (!isNull(nodeValue))
            importProfile.setPreParseRowExp(nodeValue.asText());

        nodeValue = node.get("delimiter");
        if (!isNull(nodeValue))
            importProfile.setDelimiter(nodeValue.asText());

        nodeValue = node.get("quoteCharacter");
        if (!isNull(nodeValue))
            importProfile.setQuoteCharacter(nodeValue.asText());

        nodeValue = node.get("contentType");
        if (!isNull(nodeValue)) {
            switch (nodeValue.textValue().toLowerCase()) {
                case "csv":
                    importProfile.setContentType(ExtractProfile.ContentType.CSV);
                    break;
                case "xml":
                    importProfile.setContentType(ExtractProfile.ContentType.XML);
                    break;
                case "json":
                    importProfile.setContentType(ExtractProfile.ContentType.JSON);
                    break;
            }
        }
        // ********
        // End Batch handling
        // ********

        return importProfile;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private boolean isNull(JsonNode nodeValue) {
        return nodeValue == null || nodeValue.isNull() || nodeValue.asText().equals("null");
    }

    public static ExtractProfile getImportProfile(String name, ContentModel contentModel) throws IOException {
        ExtractProfileHandler importProfile;
        ObjectMapper om = FdJsonObjectMapper.getObjectMapper();

        File fileIO = new File(name);
        if (fileIO.exists()) {
            importProfile = om.readValue(fileIO, ExtractProfileHandler.class);
            importProfile.setContentModel(contentModel);
        } else {
            InputStream stream = ClassLoader.class.getResourceAsStream(name);
            if (stream != null) {
                importProfile = om.readValue(stream, ExtractProfileHandler.class);
                importProfile.setContentModel(contentModel);
            } else
                // Defaults??
                importProfile = new ExtractProfileHandler(contentModel);
        }

        return importProfile;
    }

}
