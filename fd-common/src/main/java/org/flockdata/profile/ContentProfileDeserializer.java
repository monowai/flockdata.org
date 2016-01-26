/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

package org.flockdata.profile;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.profile.model.ContentProfile;
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
        if (nodeValue != null)
            contentProfileImpl.setDocumentName(nodeValue.asText());

        nodeValue = node.get("documentType");
        if ( nodeValue != null ) {
            nodeValue = node.get("documentType");
            contentProfileImpl.setDocumentType(mapper.readValue(nodeValue.toString(), DocumentTypeInputBean.class));
        }

        nodeValue = node.get("handler");
        if (nodeValue != null && !nodeValue.isNull())
            contentProfileImpl.setHandler(nodeValue.asText());

        nodeValue = node.get("fortressName");
        if (nodeValue != null&& !nodeValue.isNull())
            contentProfileImpl.setFortressName(nodeValue.asText());

        nodeValue = node.get("tagOrEntity");
        if ( nodeValue == null  )
            nodeValue = node.get("tagOrTrack");

        if (nodeValue != null)
            contentProfileImpl.setTagOrEntity(nodeValue.asText().equalsIgnoreCase("entity")? ContentProfile.DataType.ENTITY : ContentProfile.DataType.TAG);

        nodeValue = node.get("condition");
        if ( nodeValue!=null && ! nodeValue.isNull())
            contentProfileImpl.setCondition(nodeValue.asText());

        nodeValue = node.get("fortressUser");
        if (nodeValue != null&& !nodeValue.isNull())
            contentProfileImpl.setFortressUser(nodeValue.asText());

        nodeValue = node.get("entityOnly");
        if ( nodeValue == null || nodeValue.isNull() )
            nodeValue = node.get("metaOnly"); // legacy value
        if (nodeValue != null&& !nodeValue.isNull())
            contentProfileImpl.setEntityOnly(Boolean.parseBoolean(nodeValue.asText()));

        nodeValue = node.get("header");
        if (nodeValue != null)
            contentProfileImpl.setHeader(Boolean.parseBoolean(nodeValue.asText()));

        nodeValue = node.get("emptyIgnored");
        if (nodeValue != null)
            contentProfileImpl.setEmptyIgnored(Boolean.parseBoolean(nodeValue.asText()));


        nodeValue = node.get("preParseRowExp");
        if (nodeValue != null)
            contentProfileImpl.setPreParseRowExp(nodeValue.asText());


        nodeValue = node.get("archiveTags");
        if (nodeValue != null)
            contentProfileImpl.setArchiveTags(Boolean.parseBoolean(nodeValue.asText()));

        nodeValue = node.get("delimiter");
        if (nodeValue != null&& !nodeValue.isNull())
            contentProfileImpl.setDelimiter(nodeValue.asText());

        nodeValue = node.get("quoteCharacter");
        if (nodeValue != null&& !nodeValue.isNull())
            contentProfileImpl.setQuoteCharacter(nodeValue.asText());

        if ( nodeValue !=null && !nodeValue.isNull() )
            contentProfileImpl.setEntityKey(nodeValue.asText());

        nodeValue = node.get("event");
        if ( nodeValue != null && !nodeValue.isNull() )
            contentProfileImpl.setEvent(nodeValue.asText());

        nodeValue = node.get("segment");
        if ( nodeValue != null && !nodeValue.isNull() )
            contentProfileImpl.setSegmentExpression(nodeValue.asText());

        if ( nodeValue!=null) {
            contentProfileImpl.setEntityKey(nodeValue.asText());
        }


        nodeValue = node.get("contentType");
        if (nodeValue != null) {
            switch (nodeValue.textValue().toLowerCase()) {
                case "csv":
                    contentProfileImpl.setContentType(ContentProfile.ContentType.CSV);
                    break;
                case "xml":
                    contentProfileImpl.setContentType(ContentProfile.ContentType.XML);
                    break;
                case "json":
                    contentProfileImpl.setContentType(ContentProfile.ContentType.JSON);
                    break;
            }
        }
        nodeValue = node.get("content");
        if ( nodeValue !=null ){

            Iterator<Map.Entry<String,JsonNode>> fields = nodeValue.fields();
            Map<String,ColumnDefinition>content = new HashMap<>();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> next = fields.next();
                String colName = next.getKey();
                ColumnDefinition columnDefinition = mapper.readValue(next.getValue().toString(), ColumnDefinition.class);
//                if ( columnDefinition.getTarget()!=null )
//                    colName = columnDefinition.getTarget();
                content.put(colName, columnDefinition);
            }
            contentProfileImpl.setContent(content);
        }
        return contentProfileImpl;  //To change body of implemented methods use File | Settings | File Templates.
    }
    public static ContentProfileImpl getImportParams(String profile) throws IOException {
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
