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

package org.flockdata.track.bean;

import org.flockdata.model.Entity;
import org.flockdata.model.EntityTag;
import org.flockdata.model.MetaFortress;
import org.flockdata.search.model.SearchTag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * User: mike
 * Date: 9/07/14
 * Time: 5:01 PM
 */
public class EntityKeyBean {
    private String fortressName;
    private String index;
    private String documentType;
    private String key;
    private String code;
    private String name;
    private String description;

    private  HashMap<String, Map<String, ArrayList<SearchTag>>>  searchTags = new HashMap<>();

    private String relationship; // Entity to entity relationship

    private ACTION missingAction =ACTION.IGNORE; // default action to take when source entity to link to is missing

    private EntityKeyBean(){}

    public EntityKeyBean(String documentType, MetaFortress fortress, String code){
        this(documentType, fortress.getName(), code);
    }

    public EntityKeyBean(String documentName, String fortress, String value) {
        this.documentType = documentName;
        this.fortressName = fortress;
        this.code = value;
    }

    public EntityKeyBean(EntityInputBean entityInput) {
        this(entityInput.getDocumentType().getName(), entityInput.getFortress(), entityInput.getCode());
    }

    public EntityKeyBean(String code) {
        this.code = code;
    }

    public EntityKeyBean(EntityLinkInputBean crossReferenceInputBean) {
        this.fortressName = crossReferenceInputBean.getFortress();
        this.documentType = crossReferenceInputBean.getDocumentType();
        this.code = crossReferenceInputBean.getCode();
    }

    public EntityKeyBean(Entity entity, String index) {
        this();
        this.fortressName = entity.getSegment().getFortress().getName();
        this.code = entity.getCode();
        this.documentType = entity.getType();
        this.key = entity.getKey();
        this.index = index;
        this.name = entity.getName();
    }

    public EntityKeyBean(Entity entity, Collection<EntityTag> entityTags, String index) {
        this(entity, index);
        for (EntityTag entityTag : entityTags) {
            Map<String, ArrayList<SearchTag>> byRelationship = searchTags.get(entityTag.getRelationship());
            if ( byRelationship == null){
                byRelationship = new HashMap<>();
                String rlx = entityTag.getRelationship();

                if (rlx == null ){
                    rlx = "default";
                }
                searchTags.put(rlx.toLowerCase(), byRelationship);
            }
            ArrayList<SearchTag> tags = byRelationship.get(entityTag.getTag().getLabel().toLowerCase());
            if ( tags == null ){
                tags = new ArrayList<>();
                byRelationship.put(entityTag.getTag().getLabel().toLowerCase(), tags);
            }
            tags.add(new SearchTag(entityTag)) ;
        }
    }



    public String getFortressName() {
        return fortressName;
    }

    public String getDocumentType() {
        if ( documentType == null || documentType.equals(""))
            return "*";
        return documentType;
    }

    public String getCode() {
        return code;
    }

    public String getKey() {
        return key;
    }

    public String getIndex() {
        return index;
    }

    public HashMap<String, Map<String, ArrayList<SearchTag>>> getSearchTags() {
        return searchTags;
    }

    public String getRelationship() {
        return relationship;
    }

    public EntityKeyBean addRelationship(String relationship) {
        this.relationship = relationship;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public enum ACTION {ERROR, IGNORE, CREATE}

    public ACTION getMissingAction() {
        return missingAction;
    }

    public EntityKeyBean setMissingAction(ACTION missingAction) {
        this.missingAction = missingAction;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityKeyBean)) return false;

        EntityKeyBean entityKey = (EntityKeyBean) o;

        if (!code.equals(entityKey.code)) return false;
        if (documentType != null ? !documentType.equals(entityKey.documentType) : entityKey.documentType != null)
            return false;
        return !(fortressName != null ? !fortressName.equals(entityKey.fortressName) : entityKey.fortressName != null);

    }

    @Override
    public int hashCode() {
        int result = fortressName != null ? fortressName.hashCode() : 0;
        result = 31 * result + (documentType != null ? documentType.hashCode() : 0);
        result = 31 * result + code.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "EntityKey {" +
                "fortressName='" + fortressName + '\'' +
                ", documentType='" + documentType + '\'' +
                ", code='" + code + '\'' +
                '}';
    }

}
