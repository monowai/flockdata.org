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

package org.flockdata.track.bean;

import org.flockdata.model.DocumentType;
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
    private boolean parent;
    private String description;

    private  HashMap<String, Map<String, ArrayList<SearchTag>>>  searchTags = new HashMap<>();

    private String relationship; // Entity to resolvedEntity relationship

    private ACTION missingAction =ACTION.IGNORE; // default action to take when source resolvedEntity to link to is missing
    private Entity resolvedEntity;
    private DocumentType resolvedDocument;

    private EntityKeyBean(){}

    public EntityKeyBean(DocumentType documentType, String code) {
        this(documentType.getCode(), documentType.getFortress(), code);
        this.resolvedDocument = documentType;
    }

    public EntityKeyBean(String documentType, MetaFortress fortress, String code){
        assert (fortress.getName() !=null );
        this.documentType = documentType;
        this.fortressName = fortress.getName();
        this.code = code;
    }

    public EntityKeyBean(DocumentType documentType, MetaFortress fortress, String code) {
        this.documentType = documentType.getName();
        this.fortressName = fortress.getName();
        this.resolvedDocument = documentType;
        this.code = code;
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

    public EntityKeyBean(EntityToEntityLinkInput crossReferenceInputBean) {
        this.fortressName = crossReferenceInputBean.getFortress();
        this.documentType = crossReferenceInputBean.getDocumentType();
        this.code = crossReferenceInputBean.getCode();
    }

    public EntityKeyBean(Entity resolvedEntity, String index) {
        this();
        this.fortressName = resolvedEntity.getSegment().getFortress().getName();
        this.code = resolvedEntity.getCode();
        this.documentType = resolvedEntity.getType();
        this.key = resolvedEntity.getKey();
        this.index = index;
        this.name = resolvedEntity.getName();
    }

    public EntityKeyBean(Entity resolvedEntity, Collection<EntityTag> entityTags, String index) {
        this(resolvedEntity, index);
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

    public EntityKeyBean(String code, String documentType) {
        this.code = code;
        this.documentType = documentType;
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

    /**
     * Flags this EntityKeyBean as a parent of the EntityBean it is being tracked against
     * @param parent yes - create an outbound relationship
     * @return this
     */
    public EntityKeyBean setParent(boolean parent) {
        this.parent = parent;
        return this;
    }

    public boolean isParent() {
        return parent;
    }

    public EntityKeyBean setRelationship(String relationship) {
        this.relationship = relationship;
        return this;
    }

    public Entity getResolvedEntity() {
        return resolvedEntity;
    }

    public DocumentType getResolvedDocument() {
        return resolvedDocument;
    }

    public void setResolvedDocument(DocumentType documentType) {
        this.resolvedDocument = documentType;
    }

    public void setResolvedEntity(Entity resolvedEntity) {
        this.resolvedEntity = resolvedEntity;
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
