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

        nodeValue = node.get("code");
        if (!isNull(nodeValue))
            contentModel.setCode(nodeValue.asText());

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

        nodeValue = node.get("tagModel");
        if (!isNull(nodeValue))
            contentModel.setTagModel(Boolean.parseBoolean(nodeValue.asText()));

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

    /**
     * Resolves a content model from disk
     * @param fileName file
     * @return null if not found otherwise the model content
     * @throws IOException
     */
    public static ContentModel getContentModel(String fileName) throws IOException {
        ContentModel contentModel = null;
        ObjectMapper om = FdJsonObjectMapper.getObjectMapper();

        File fileIO = new File(fileName);
        if (fileIO.exists()) {
            contentModel = om.readValue(fileIO, ContentModelHandler.class);

        } else {
            InputStream stream = ClassLoader.class.getResourceAsStream(fileName);
            if (stream != null) {
                contentModel = om.readValue(stream, ContentModelHandler.class);

            }

        }
        return contentModel;
    }

}
