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

import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.profile.model.ProfileConfiguration;
import org.flockdata.transform.ColumnDefinition;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
public class ImportProfileDeserializer extends JsonDeserializer<ImportProfile> {
    @Override
    public ImportProfile deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ImportProfile importProfile = new ImportProfile();
        JsonNode node = jp.getCodec().readTree(jp);
        JsonNode nodeValue = node.get("documentName");
        if ( nodeValue == null )
            nodeValue = node.get("documentType");
        if (nodeValue != null)
            importProfile.setDocumentName(nodeValue.asText());

        nodeValue = node.get("handler");
        if (nodeValue != null && !nodeValue.isNull())
            importProfile.setHandler(nodeValue.asText());

        nodeValue = node.get("fortressName");
        if (nodeValue != null&& !nodeValue.isNull())
            importProfile.setFortressName(nodeValue.asText());

        nodeValue = node.get("tagOrEntity");
        if ( nodeValue == null  )
            nodeValue = node.get("tagOrTrack");

        if (nodeValue != null)
            importProfile.setTagOrEntity(nodeValue.asText().equalsIgnoreCase("entity")? ProfileConfiguration.DataType.ENTITY : ProfileConfiguration.DataType.TAG);

        nodeValue = node.get("condition");
        if ( nodeValue!=null && ! nodeValue.isNull())
            importProfile.setCondition(nodeValue.asText());

        nodeValue = node.get("fortressUser");
        if (nodeValue != null&& !nodeValue.isNull())
            importProfile.setFortressUser(nodeValue.asText());

        nodeValue = node.get("entityOnly");
        if ( nodeValue != null&& !nodeValue.isNull() )
            nodeValue = node.get("metaOnly"); // legacy value
        if (nodeValue != null&& !nodeValue.isNull())
            importProfile.setEntityOnly(Boolean.parseBoolean(nodeValue.asText()));

        nodeValue = node.get("header");
        if (nodeValue != null)
            importProfile.setHeader(Boolean.parseBoolean(nodeValue.asText()));

        nodeValue = node.get("emptyIgnored");
        if (nodeValue != null)
            importProfile.setEmptyIgnored(Boolean.parseBoolean(nodeValue.asText()));


        nodeValue = node.get("preParseRowExp");
        if (nodeValue != null)
            importProfile.setPreParseRowExp(nodeValue.asText());


        nodeValue = node.get("archiveTags");
        if (nodeValue != null)
            importProfile.setArchiveTags(Boolean.parseBoolean(nodeValue.asText()));

        nodeValue = node.get("delimiter");
        if (nodeValue != null&& !nodeValue.isNull())
            importProfile.setDelimiter(nodeValue.asText());

        nodeValue = node.get("quoteCharacter");
        if (nodeValue != null&& !nodeValue.isNull())
            importProfile.setQuoteCharacter(nodeValue.asText());

        if ( nodeValue !=null && !nodeValue.isNull() )
            importProfile.setEntityKey(nodeValue.asText());

        nodeValue = node.get("event");
        if ( nodeValue != null && !nodeValue.isNull() )
            importProfile.setEvent(nodeValue.asText());

        nodeValue = node.get("segment");
        if ( nodeValue != null && !nodeValue.isNull() )
            importProfile.setSegmentExpression(nodeValue.asText());

        if ( nodeValue!=null) {
            importProfile.setEntityKey(nodeValue.asText());
        }


        nodeValue = node.get("contentType");
        if (nodeValue != null) {
            switch (nodeValue.textValue().toLowerCase()) {
                case "csv":
                    importProfile.setContentType(ProfileConfiguration.ContentType.CSV);
                    break;
                case "xml":
                    importProfile.setContentType(ProfileConfiguration.ContentType.XML);
                    break;
                case "json":
                    importProfile.setContentType(ProfileConfiguration.ContentType.JSON);
                    break;
            }
        }
        nodeValue = node.get("content");
        if ( nodeValue !=null ){
            ObjectMapper mapper = FdJsonObjectMapper.getObjectMapper();
            Iterator<Map.Entry<String,JsonNode>> columnNodes = nodeValue.fields();
            Map<String,ColumnDefinition>content = new HashMap<>();
            while (columnNodes.hasNext()) {
                Map.Entry<String, JsonNode> next = columnNodes.next();
                String colName = next.getKey();
                ColumnDefinition columnDefinition = mapper.readValue(next.getValue().toString(), ColumnDefinition.class);
//                if ( columnDefinition.getTarget()!=null )
//                    colName = columnDefinition.getTarget();
                content.put(colName, columnDefinition);
            }
            importProfile.setContent(content);
        }
        return importProfile;  //To change body of implemented methods use File | Settings | File Templates.
    }
    public static ImportProfile getImportParams(String profile) throws IOException {
        ImportProfile importProfile;
        ObjectMapper om = FdJsonObjectMapper.getObjectMapper();

        File fileIO = new File(profile);
        if (fileIO.exists()) {
            importProfile = om.readValue(fileIO, ImportProfile.class);

        } else {
            InputStream stream = ClassLoader.class.getResourceAsStream(profile);
            if (stream != null) {
                importProfile = om.readValue(stream, ImportProfile.class);
            } else
                // Defaults??
                importProfile = new ImportProfile();
        }
        //importParams.setRestClient(restClient);
        return importProfile;
    }

}
