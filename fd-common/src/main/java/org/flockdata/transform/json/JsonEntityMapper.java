/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.transform.json;

import com.fasterxml.jackson.databind.JsonNode;
import org.flockdata.profile.model.Mappable;
import org.flockdata.profile.model.ProfileConfiguration;
import org.flockdata.registration.bean.TagInputBean;
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
public class JsonEntityMapper extends EntityInputBean implements Mappable {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(JsonEntityMapper.class);

    @Override
    public ProfileConfiguration.ContentType getImporter() {
        return ProfileConfiguration.ContentType.JSON;
    }

    public void setData(JsonNode node, ProfileConfiguration profile) {
        for (Map.Entry<String, ColumnDefinition> entry : profile.getContent().entrySet()) {
            JsonNode nodeField = node.get(entry.getKey());
            if ( nodeField!=null )
                if ( nodeField.isArray())
                    handleArray(nodeField, entry.getValue());
                else {
                    ColumnDefinition colDef = profile.getColumnDef(entry.getKey());
                    if ( colDef != null ) {
                        if ( colDef.isCallerRef())
                            setCallerRef(nodeField.asText());
                        if ( colDef.isTitle())
                            setName(nodeField.asText());
                        if ( colDef.isDescription())
                            setDescription(nodeField.asText());
                        if ( colDef.isDocument())
                            setDocumentName(nodeField.asText());
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
                if (!thisNode.get(rlx.getSourceProperty()).hasNonNull(rlx.getSourceProperty()))
                    rlxProperties.put(rlx.getTargetProperty(), thisNode.get(rlx.getSourceProperty()).textValue());
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
