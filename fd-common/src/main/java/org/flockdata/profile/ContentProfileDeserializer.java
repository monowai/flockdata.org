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
import org.flockdata.profile.model.ContentProfile;
import org.flockdata.profile.model.ImportFile;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.transform.ColumnDefinition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * User: mike
 * Date: 9/05/14
 * Time: 8:45 AM
 */
public class ContentProfileDeserializer extends JsonDeserializer<ContentProfileImpl> {

    ObjectMapper mapper = FdJsonObjectMapper.getObjectMapper();

    @Override
    public ContentProfileImpl deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ContentProfileImpl contentProfileImpl = new ContentProfileImpl();
        JsonNode node = jp.getCodec().readTree(jp);
        JsonNode nodeValue = node.get("documentName");

        if (!isNull(nodeValue))
            contentProfileImpl.setDocumentName(nodeValue.asText());

        nodeValue = node.get("documentType");
        if (!isNull(nodeValue)){
            nodeValue = node.get("documentType");
            contentProfileImpl.setDocumentType(mapper.readValue(nodeValue.toString(), DocumentTypeInputBean.class));
        }

        nodeValue = node.get("fortressName");
        if (!isNull(nodeValue))
            contentProfileImpl.setFortress(new FortressInputBean(nodeValue.asText()));

        nodeValue = node.get("fortress");
        if (!isNull(nodeValue)){
            nodeValue = node.get("fortress");
            contentProfileImpl.setFortress(mapper.readValue(nodeValue.toString(), FortressInputBean.class));
        }

        nodeValue = node.get("tagOrEntity");
        if ( nodeValue == null  )
            nodeValue = node.get("tagOrTrack");

        if (!isNull(nodeValue))
            contentProfileImpl.setTagOrEntity(nodeValue.asText().equalsIgnoreCase("entity")? ContentProfile.DataType.ENTITY : ContentProfile.DataType.TAG);

        nodeValue = node.get("name");
        if (!isNull(nodeValue))
            contentProfileImpl.setName(nodeValue.asText());

        nodeValue = node.get("condition");
        if (!isNull(nodeValue))
            contentProfileImpl.setCondition(nodeValue.asText());

        nodeValue = node.get("fortressUser");
        if (!isNull(nodeValue))
            contentProfileImpl.setFortressUser(nodeValue.asText());

        nodeValue = node.get("entityOnly");
        if (isNull(nodeValue))
            nodeValue = node.get("metaOnly"); // legacy value

        if (!isNull(nodeValue))
            contentProfileImpl.setEntityOnly(Boolean.parseBoolean(nodeValue.asText()));

        nodeValue = node.get("emptyIgnored");
        if (!isNull(nodeValue))
            contentProfileImpl.setEmptyIgnored(Boolean.parseBoolean(nodeValue.asText()));

        nodeValue = node.get("archiveTags");
        if (!isNull(nodeValue))
            contentProfileImpl.setArchiveTags(Boolean.parseBoolean(nodeValue.asText()));

        nodeValue = node.get("event");
        if (!isNull(nodeValue))
            contentProfileImpl.setEvent(nodeValue.asText());

        nodeValue = node.get("segment");
        if (!isNull(nodeValue))
            contentProfileImpl.setSegmentExpression(nodeValue.asText());

        nodeValue = node.get("content");
        if (!isNull(nodeValue)){

            Iterator<Map.Entry<String,JsonNode>> fields = nodeValue.fields();
            Map<String,ColumnDefinition>content = new HashMap<>();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> next = fields.next();
                String colName = next.getKey();
                ColumnDefinition columnDefinition = mapper.readValue(next.getValue().toString(), ColumnDefinition.class);
                content.put(colName, columnDefinition);
            }
            contentProfileImpl.setContent(content);
        }

        // ********
        // Batch handling
        // ********
        nodeValue = node.get("handler");
        if (!isNull(nodeValue))
            contentProfileImpl.setHandler(nodeValue.asText());

        nodeValue = node.get("header");
        if (!isNull(nodeValue))
            contentProfileImpl.setHeader(Boolean.parseBoolean(nodeValue.asText()));

        nodeValue = node.get("preParseRowExp");
        if (!isNull(nodeValue))
            contentProfileImpl.setPreParseRowExp(nodeValue.asText());

        nodeValue = node.get("delimiter");
        if (!isNull(nodeValue))
            contentProfileImpl.setDelimiter(nodeValue.asText());

        nodeValue = node.get("quoteCharacter");
        if (!isNull(nodeValue))
            contentProfileImpl.setQuoteCharacter(nodeValue.asText());

        nodeValue = node.get("contentType");
        if (!isNull(nodeValue)){
            switch (nodeValue.textValue().toLowerCase()) {
                case "csv":
                    contentProfileImpl.setContentType(ImportFile.ContentType.CSV);
                    break;
                case "xml":
                    contentProfileImpl.setContentType(ImportFile.ContentType.XML);
                    break;
                case "json":
                    contentProfileImpl.setContentType(ImportFile.ContentType.JSON);
                    break;
            }
        }
        // ********
        // End Batch handling
        // ********

        return contentProfileImpl;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private boolean isNull(JsonNode nodeValue) {
        return nodeValue == null || nodeValue.isNull() || nodeValue.asText().equals("null");
    }

    public static ContentProfileImpl getContentProfile(String profile) throws IOException {
        ContentProfileImpl contentProfileImpl;
        ObjectMapper om = FdJsonObjectMapper.getObjectMapper();

        File fileIO = new File(profile);
        if (fileIO.exists()) {
            contentProfileImpl = om.readValue(fileIO, ContentProfileImpl.class);

        } else {
            InputStream stream = ClassLoader.class.getResourceAsStream(profile);
            if (stream != null) {
                contentProfileImpl = om.readValue(stream, ContentProfileImpl.class);
            } else
                // Defaults??
                contentProfileImpl = new ContentProfileImpl();
        }
        //importParams.setRestClient(restClient);
        return contentProfileImpl;
    }

}
