/*
 *  Copyright 2012-2017 the original author or authors.
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

package org.flockdata.transform.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.flockdata.data.ContentModel;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.model.ContentModelHandler;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * JSON deserializer
 *
 * @author mholdsworth
 * @tag ContentModel, Json
 * @since 24/06/2016
 */
public class ContentModelDeserializer extends JsonDeserializer<ContentModel> {
  private static final ObjectMapper mapper = new ObjectMapper(new FdJsonObjectMapper())
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .enable(JsonParser.Feature.ALLOW_COMMENTS);

  /**
   * Resolves a content model from disk
   *
   * @param fileName file
   * @return null if not found otherwise the model content
   * @throws IOException Issue with JSON
   */
  public static ContentModel getContentModel(String fileName) throws IOException {
    ContentModel contentModel = null;
    ObjectMapper om = FdJsonObjectMapper.getObjectMapper();
    String trimmedFile = StringUtils.trimLeadingWhitespace(fileName.trim());

    File fileIO = new File(trimmedFile);
    if (fileIO.exists()) {
      contentModel = om.readValue(fileIO, ContentModelHandler.class);

    } else {
      Resource resource = new ClassPathResource(trimmedFile);
      InputStream stream = resource.getInputStream();
      contentModel = om.readValue(stream, ContentModelHandler.class);
    }
    return contentModel;
  }

  @Override
  public ContentModel deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    ContentModelHandler contentModel = new ContentModelHandler();
    JsonNode node = jp.getCodec().readTree(jp);
    JsonNode nodeValue = node.get("documentName");

    if (!isNull(nodeValue)) {
      contentModel.setDocumentName(nodeValue.asText());
    }

    nodeValue = node.get("documentType");
    if (!isNull(nodeValue)) {
      nodeValue = node.get("documentType");
      contentModel.setDocumentType(mapper.readValue(nodeValue.toString(), DocumentTypeInputBean.class));
    }

    nodeValue = node.get("handler");
    if (!isNull(nodeValue)) {
      contentModel.setHandler(nodeValue.asText());
    }

    nodeValue = node.get("code");
    if (!isNull(nodeValue)) {
      contentModel.setCode(nodeValue.asText());
    }

    nodeValue = node.get("fortressName");
    if (!isNull(nodeValue)) {
      contentModel.setFortress(new FortressInputBean(nodeValue.asText()));
    }

    nodeValue = node.get("fortress");
    if (!isNull(nodeValue)) {
      nodeValue = node.get("fortress");
      contentModel.setFortress(mapper.readValue(nodeValue.toString(), FortressInputBean.class));
    }

    nodeValue = node.get("name");
    if (!isNull(nodeValue)) {
      contentModel.setName(nodeValue.asText());
    }

    nodeValue = node.get("condition");
    if (!isNull(nodeValue)) {
      contentModel.setCondition(nodeValue.asText());
    }

    nodeValue = node.get("emptyIgnored");
    if (!isNull(nodeValue)) {
      contentModel.setEmptyIgnored(Boolean.parseBoolean(nodeValue.asText()));
    }

    nodeValue = node.get("tagModel");
    if (!isNull(nodeValue)) {
      contentModel.setTagModel(Boolean.parseBoolean(nodeValue.asText()));
    }

    nodeValue = node.get("entityOnly");
    if (isNull(nodeValue)) {
      nodeValue = node.get("metaOnly"); // legacy value
    }

    if (!isNull(nodeValue)) {
      contentModel.setEntityOnly(Boolean.parseBoolean(nodeValue.asText()));
    }

    nodeValue = node.get("archiveTags");
    if (!isNull(nodeValue)) {
      contentModel.setArchiveTags(Boolean.parseBoolean(nodeValue.asText()));
    }

    nodeValue = node.get("event");
    if (!isNull(nodeValue)) {
      contentModel.setEvent(nodeValue.asText());
    }

    nodeValue = node.get("segment");
    if (!isNull(nodeValue)) {
      contentModel.setSegmentExpression(nodeValue.asText());
    }

    nodeValue = node.get("trackSuppressed");
    if (!isNull(nodeValue)) {
      contentModel.setTrackSuppressed(nodeValue.asBoolean());
    }

    nodeValue = node.get("searchSuppressed");
    if (!isNull(nodeValue)) {
      contentModel.setSearchSuppressed(nodeValue.asBoolean());
    }

    nodeValue = node.get("content");
    if (!isNull(nodeValue)) {

      Iterator<Map.Entry<String, JsonNode>> fields = nodeValue.fields();
      Map<String, ColumnDefinition> content = new HashMap<>();
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

}
