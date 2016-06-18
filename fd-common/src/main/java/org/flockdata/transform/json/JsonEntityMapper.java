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

package org.flockdata.transform.json;

import com.fasterxml.jackson.databind.JsonNode;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.tags.TagProfile;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * User: mike
 * Date: 9/12/14
 * Time: 6:15 PM
 */
public class JsonEntityMapper extends EntityInputBean  {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(JsonEntityMapper.class);

    public void setData(JsonNode node, ContentModel profile) {
        for (Map.Entry<String, ColumnDefinition> entry : profile.getContent().entrySet()) {
            JsonNode nodeField = node.get(entry.getKey());
            if ( nodeField!=null )
                if ( nodeField.isArray())
                    handleArray(nodeField, entry.getValue());
                else {
                    ColumnDefinition colDef = profile.getColumnDef(entry.getKey());
                    if ( colDef != null ) {
                        if ( colDef.isCallerRef())
                            setCode(nodeField.asText());
                        if ( colDef.isTitle())
                            setName(nodeField.asText());
                        if ( colDef.isDescription())
                            setDescription(nodeField.asText());
                        if ( colDef.isDocument())
                            setDocumentType(new DocumentTypeInputBean(nodeField.asText()));
                        if ( colDef.isTag())
                            addTag(getTagFromNode(nodeField, colDef));

                    }
                }
        }
        logger.debug("entity calcd {}", this.toString());
    }

    private void handleArray(JsonNode arrayValues, ColumnDefinition colDef){
        Collection<TagInputBean>results = new ArrayList<>();
        Iterator<JsonNode>nodes = arrayValues.elements();
        while (nodes.hasNext()){
            JsonNode thisNode = nodes.next();
            results.add(getTagFromNode(thisNode, colDef));
        }

        setTags(results);

    }

    private TagInputBean getTagFromNode(JsonNode thisNode, ColumnDefinition colDef) {
        String code;
        boolean readRow = true;
        if (colDef.isArrayDelimited()){
            code = thisNode.asText();
            readRow = false;
        } else if (colDef.getCode() == null ) {
            code = thisNode.asText();
            readRow=false;
        } else
            code= thisNode.get(colDef.getCode()).asText();

        TagInputBean tag = new TagInputBean(code);

        if (readRow)
            tag.setName(thisNode.get(colDef.getName()).asText());

        tag.setLabel(colDef.getLabel());
        Map<String,Object> rlxProperties = new HashMap<>();

        if ( colDef.hasRelationshipProps() ) {
            for (ColumnDefinition rlx : colDef.getRlxProperties()) {
                if (!thisNode.get(rlx.getSource()).hasNonNull(rlx.getSource()))
                    rlxProperties.put(rlx.getTarget(), thisNode.get(rlx.getSource()).textValue());
            }
        }
        setSubTags(tag, colDef.getTargets(), thisNode);
        String rlx = colDef.getRelationship();

        if ( rlx==null )
            rlx = "undefined";

        tag.addEntityLink(rlx, rlxProperties);
        return tag;
    }

    private void setSubTags(TagInputBean parentTag, ArrayList<TagProfile> subTags, JsonNode jsonNode){
        if ( subTags != null && !subTags.isEmpty()) {
            for (TagProfile subTag : subTags) {
                String codeValue = jsonNode.get(subTag.getCode()).textValue();
                if ( codeValue !=null ) {
                    TagInputBean tagInputBean = new TagInputBean(codeValue);
                    tagInputBean.setLabel(subTag.getLabel());
                    String rlx = subTag.getRelationship();
                    if ( rlx==null )
                        rlx = "undefined";

                    parentTag.setTargets(rlx, tagInputBean);
                    setSubTags(tagInputBean, subTag.getTargets(), jsonNode);
                }
            }

        }

    }

}
