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
 * JSON deserializer
 *
 * Created by mike on 24/06/16.
 */
public class ContentModelDeserializer extends JsonDeserializer<ContentModel> {
    ObjectMapper mapper = FdJsonObjectMapper.getObjectMapper();

    @Override
    public ContentModel deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ContentModelHandler contentModel = new ContentModelHandler();
        JsonNode node = jp.getCodec().readTree(jp);
        JsonNode nodeValue = node.get("documentName");

        if (!isNull(nodeValue))
            contentModel.setDocumentName(nodeValue.asText());

        nodeValue = node.get("documentType");
        if (!isNull(nodeValue)){
            nodeValue = node.get("documentType");
            contentModel.setDocumentType(mapper.readValue(nodeValue.toString(), DocumentTypeInputBean.class));
        }

        nodeValue = node.get("handler");
        if (!isNull(nodeValue))
            contentModel.setHandler(nodeValue.asText());

        nodeValue = node.get("fortressName");
        if (!isNull(nodeValue))
            contentModel.setFortress(new FortressInputBean(nodeValue.asText()));

        nodeValue = node.get("fortress");
        if (!isNull(nodeValue)){
            nodeValue = node.get("fortress");
            contentModel.setFortress(mapper.readValue(nodeValue.toString(), FortressInputBean.class));
        }

        nodeValue = node.get("name");
        if (!isNull(nodeValue))
            contentModel.setName(nodeValue.asText());

        nodeValue = node.get("condition");
        if (!isNull(nodeValue))
            contentModel.setCondition(nodeValue.asText());

        nodeValue = node.get("emptyIgnored");
        if (!isNull(nodeValue))
            contentModel.setEmptyIgnored(Boolean.parseBoolean(nodeValue.asText()));

        nodeValue = node.get("entityOnly");
        if (isNull(nodeValue))
            nodeValue = node.get("metaOnly"); // legacy value

        if (!isNull(nodeValue))
            contentModel.setEntityOnly(Boolean.parseBoolean(nodeValue.asText()));

        nodeValue = node.get("archiveTags");
        if (!isNull(nodeValue))
            contentModel.setArchiveTags(Boolean.parseBoolean(nodeValue.asText()));

        nodeValue = node.get("event");
        if (!isNull(nodeValue))
            contentModel.setEvent(nodeValue.asText());

        nodeValue = node.get("segment");
        if (!isNull(nodeValue))
            contentModel.setSegmentExpression(nodeValue.asText());

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
            contentModel.setContent(content);
        }

        return contentModel;
    }
    private boolean isNull(JsonNode nodeValue) {
        return nodeValue == null || nodeValue.isNull() || nodeValue.asText().equals("null");
    }

    public static ContentModel getContentModel(String profile) throws IOException {
        ContentModel contentModel;
        ObjectMapper om = FdJsonObjectMapper.getObjectMapper();

        File fileIO = new File(profile);
        if (fileIO.exists()) {
            contentModel = om.readValue(fileIO, ContentModelHandler.class);

        } else {
            InputStream stream = ClassLoader.class.getResourceAsStream(profile);
            if (stream != null) {
                contentModel = om.readValue(stream, ContentModelHandler.class);
            } else
                // Defaults??
                contentModel = new ContentModelHandler();
        }
        //importParams.setRestClient(restClient);
        return contentModel;
    }

}
