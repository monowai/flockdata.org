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

package org.flockdata.transform.entity;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.flockdata.data.ContentModel;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityTagRelationshipDefinition;
import org.flockdata.track.bean.EntityTagRelationshipInput;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.TransformationHelper;
import org.flockdata.transform.tag.TagProfile;
import org.slf4j.LoggerFactory;

/**
 * @author mholdsworth
 * @since 9/12/2014
 */
public class JsonEntityTransformer extends EntityInputBean {

  private static org.slf4j.Logger logger = LoggerFactory.getLogger(JsonEntityTransformer.class);

  public void setData(JsonNode node, ContentModel profile) {
    for (Map.Entry<String, ColumnDefinition> entry : profile.getContent().entrySet()) {
      JsonNode nodeField = node.get(entry.getKey());
      if (nodeField != null) {
        if (nodeField.isArray()) {
          handleArray(nodeField, entry.getValue());
        } else {
          ColumnDefinition colDef = profile.getColumnDef(entry.getKey());
          if (colDef != null) {
            if (TransformationHelper.evaluate(colDef.isCallerRef(), false)) {
              setCode(nodeField.asText());
            }
            if (TransformationHelper.evaluate(colDef.isTitle(), false)) {
              setName(nodeField.asText());
            }
            if (TransformationHelper.evaluate(colDef.isDescription(), false)) {
              setDescription(nodeField.asText());
            }
            if (TransformationHelper.evaluate(colDef.isDocument(), false)) {
              setDocumentType(new DocumentTypeInputBean(nodeField.asText()));
            }
            if (TransformationHelper.evaluate(colDef.isTag(), false)) {
              addTag(getTagFromNode(nodeField, colDef));
            }

          }
        }
      }
    }
    logger.debug("entity calcd {}", this.toString());
  }

  private void handleArray(JsonNode arrayValues, ColumnDefinition colDef) {
    Collection<TagInputBean> results = new ArrayList<>();
    Iterator<JsonNode> nodes = arrayValues.elements();
    while (nodes.hasNext()) {
      JsonNode thisNode = nodes.next();
      results.add(getTagFromNode(thisNode, colDef));
    }

    setTags(results);

  }

  private TagInputBean getTagFromNode(JsonNode thisNode, ColumnDefinition colDef) {
    String code;
    boolean readRow = true;
    if (colDef.isArrayDelimited()) {
      code = thisNode.asText();
      readRow = false;
    } else if (colDef.getCode() == null) {
      code = thisNode.asText();
      readRow = false;
    } else {
      code = thisNode.get(colDef.getCode()).asText();
    }

    TagInputBean tag = new TagInputBean(code);

    if (readRow) {
      tag.setName(thisNode.get(colDef.getName()).asText());
    }

    tag.setLabel(colDef.getLabel());
    Map<String, Object> rlxProperties = new HashMap<>();

    if (colDef.hasRelationshipProps()) {
      for (ColumnDefinition rlx : colDef.getRlxProperties()) {
        if (!thisNode.get(rlx.getSource()).hasNonNull(rlx.getSource())) {
          rlxProperties.put(rlx.getTarget(), thisNode.get(rlx.getSource()).textValue());
        }
      }
    }
    setSubTags(tag, colDef.getTargets(), thisNode);
    Collection<EntityTagRelationshipDefinition> links = colDef.getEntityTagLinks();
    String rlx;
    boolean geo = false;
    if (links == null || links.isEmpty()) {
      rlx = "default";
    } else {
      EntityTagRelationshipDefinition etr = links.iterator().next();
      geo = etr.getGeo();
      rlx = links.iterator().next().getRelationshipName();
    }

    tag.addEntityTagLink(new EntityTagRelationshipInput(rlx, geo));
    return tag;
  }

  private void setSubTags(TagInputBean parentTag, ArrayList<TagProfile> subTags, JsonNode jsonNode) {
    if (subTags != null && !subTags.isEmpty()) {
      for (TagProfile subTag : subTags) {
        String codeValue = jsonNode.get(subTag.getCode()).textValue();
        if (codeValue != null) {
          TagInputBean tagInputBean = new TagInputBean(codeValue);
          tagInputBean.setLabel(subTag.getLabel());
          String rlx = subTag.getRelationship();
          if (rlx == null) {
            rlx = "undefined";
          }

          parentTag.setTargets(rlx, tagInputBean);
          setSubTags(tagInputBean, subTag.getTargets(), jsonNode);
        }
      }

    }

  }

}
