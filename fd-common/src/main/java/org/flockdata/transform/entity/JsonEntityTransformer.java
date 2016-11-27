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

package org.flockdata.transform.entity;

import com.fasterxml.jackson.databind.JsonNode;
import org.flockdata.model.EntityTagRelationshipDefinition;
import org.flockdata.model.EntityTagRelationshipInput;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.TransformationHelper;
import org.flockdata.transform.tag.TagProfile;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author mholdsworth
 * @since 9/12/2014
 */
public class JsonEntityTransformer extends EntityInputBean  {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(JsonEntityTransformer.class);

    public void setData(JsonNode node, ContentModel profile) {
        for (Map.Entry<String, ColumnDefinition> entry : profile.getContent().entrySet()) {
            JsonNode nodeField = node.get(entry.getKey());
            if ( nodeField!=null )
                if ( nodeField.isArray())
                    handleArray(nodeField, entry.getValue());
                else {
                    ColumnDefinition colDef = profile.getColumnDef(entry.getKey());
                    if ( colDef != null ) {
                        if (TransformationHelper.evaluate(colDef.isCallerRef(), false))
                            setCode(nodeField.asText());
                        if (TransformationHelper.evaluate(colDef.isTitle(), false))
                            setName(nodeField.asText());
                        if (TransformationHelper.evaluate(colDef.isDescription(), false))
                            setDescription(nodeField.asText());
                        if (TransformationHelper.evaluate(colDef.isDocument(), false))
                            setDocumentType(new DocumentTypeInputBean(nodeField.asText()));
                        if (TransformationHelper.evaluate(colDef.isTag(), false))
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
        Collection<EntityTagRelationshipDefinition> links = colDef.getEntityTagLinks();
        String rlx ;
        boolean geo = false;
        if ( links == null || links.isEmpty())
            rlx = "default";
        else {
            EntityTagRelationshipDefinition etr = links.iterator().next();
            geo = etr.getGeo();
            rlx = links.iterator().next().getRelationshipName();
        }

        tag.addEntityTagLink(rlx, new EntityTagRelationshipInput(rlx,geo));
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
