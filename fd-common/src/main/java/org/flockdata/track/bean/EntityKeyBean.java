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

package org.flockdata.track.bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.flockdata.data.Document;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityTag;
import org.flockdata.data.Fortress;
import org.flockdata.search.SearchTag;

/**
 * Links an Entity to another Entity with user supplied keys
 *
 * @author mholdsworth
 * @tag Track, Contract, EntityKey
 * @since 9/07/2014
 */
public class EntityKeyBean {
  private String relationshipName; // Entity to Entity relationship name
  @JsonProperty("fortress")
  private String fortressName;
  @JsonProperty("documentName")
  private String documentType;
  private String index;
  private String key;
  private String code;
  private String name;
  private boolean parent;

  private Document resolvedDocument;

  private String description;

  private HashMap<String, Map<String, ArrayList<SearchTag>>> searchTags = new HashMap<>();

  private ACTION missingAction = ACTION.IGNORE; // default action to take when source resolvedEntity to link to is missing
  private Entity resolvedEntity;

  private EntityKeyBean() {
  }

  public EntityKeyBean(String documentType, Fortress fortress, String code) {
    assert (fortress.getName() != null);
    this.documentType = documentType;
    this.fortressName = fortress.getName();
    this.code = code;
  }

  /**
   * Convenience function for testing purposes
   *
   * @param documentType
   * @param fortress
   * @param code
   * @param relationshipName
   */
  public EntityKeyBean(String documentType, Fortress fortress, String code, String relationshipName) {
    this.documentType = documentType;
    this.fortressName = fortress.getName();
    this.code = code;
    this.relationshipName = relationshipName;
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
      if (byRelationship == null) {
        byRelationship = new HashMap<>();
        String rlx = entityTag.getRelationship();

        if (rlx == null) {
          rlx = "default";
        }
        searchTags.put(rlx.toLowerCase(), byRelationship);
      }
      ArrayList<SearchTag> tags = byRelationship.get(entityTag.getTag().getLabel().toLowerCase());
      if (tags == null) {
        tags = new ArrayList<>();
        byRelationship.put(entityTag.getTag().getLabel().toLowerCase(), tags);
      }
      tags.add(new SearchTag(entityTag));
    }
  }

  @Deprecated // caller should supply the relationship name
  public EntityKeyBean(String code, String documentType) {
    this.code = code;
    this.documentType = documentType;
  }


  public String getFortressName() {
    return fortressName;
  }

  public String getDocumentType() {
    if (documentType == null || documentType.equals("")) {
      return "*";
    }
    return documentType;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String getCode() {
    return code;
  }

  public EntityKeyBean setCode(String code) {
    this.code = code;
    return this;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String getKey() {
    return key;
  }

  public String getIndex() {
    return index;
  }

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public HashMap<String, Map<String, ArrayList<SearchTag>>> getSearchTags() {
    return searchTags;
  }

  public String getRelationshipName() {
    return relationshipName;
  }

  public EntityKeyBean setRelationshipName(String relationshipName) {
    this.relationshipName = relationshipName;
    return this;
  }

  public EntityKeyBean addRelationship(String relationship) {
    this.relationshipName = relationship;
    return this;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String getDescription() {
    return description;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String getName() {
    return name;
  }

  public boolean isParent() {
    return parent;
  }

  /**
   * Flags this EntityKeyBean as a parent of the EntityBean it is being tracked against
   *
   * @param parent yes - create an outbound relationship
   * @return this
   */
  public EntityKeyBean setParent(boolean parent) {
    this.parent = parent;
    return this;
  }

  public Entity getResolvedEntity() {
    return resolvedEntity;
  }

  public void setResolvedEntity(Entity resolvedEntity) {
    this.resolvedEntity = resolvedEntity;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public Document getResolvedDocument() {
    return resolvedDocument;
  }

  public void setResolvedDocument(Document documentType) {
    this.resolvedDocument = documentType;
  }

  public ACTION getMissingAction() {
    return missingAction;
  }

  public EntityKeyBean setMissingAction(ACTION missingAction) {
    this.missingAction = missingAction;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EntityKeyBean)) {
      return false;
    }

    EntityKeyBean entityKey = (EntityKeyBean) o;

    if (!code.equals(entityKey.code)) {
      return false;
    }
    if (documentType != null ? !documentType.equals(entityKey.documentType) : entityKey.documentType != null) {
      return false;
    }
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

  public enum ACTION {ERROR, IGNORE, CREATE}

}
