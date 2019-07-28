/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

package org.flockdata.transform.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;
import org.flockdata.data.ContentModel;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.json.ContentModelDeserializer;

/**
 * How data is transformed
 *
 * @author mholdsworth
 * @since 24/06/2016
 */

@JsonDeserialize(using = ContentModelDeserializer.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContentModelHandler implements ContentModel {
  protected DocumentTypeInputBean documentType = null;

  private String fortressName = null;
  private String name = null; // User supplied description of this profile
  private String code = null; // Mandatory for tags and null for entity
  private FortressInputBean fortress = null;
  private Boolean entityOnly = null;
  private Boolean archiveTags = true;
  private String event = null;
  private Boolean emptyIgnored = null;
  private Map<String, Object> properties;
  private String segmentExpression;
  private String fortressUser;
  private String handler;
  private String condition;
  private Boolean trackSuppressed;
  private Boolean searchSuppressed;
  private boolean tagModel;

  private Map<String, ColumnDefinition> content;

  @Override
  public FortressInputBean getFortress() {
    return fortress;
  }

  @Override
  public String getHandler() {
    return handler;
  }

  public void setHandler(String handler) {
    this.handler = handler;
  }

  @Deprecated // use setFortress and provide default properties
  @Override
  public void setFortressName(String fortressName) {
    fortress = new FortressInputBean(fortressName);
  }

  @Override
  public Boolean isEntityOnly() {
    return entityOnly;
  }

  public void setEntityOnly(boolean entityOnly) {
    this.entityOnly = entityOnly;
  }

  @Override
  public ColumnDefinition getColumnDef(String column) {
    if (content == null) {
      return null;
    }
    return content.get(column);
  }

  @Override
  public Map<String, ColumnDefinition> getContent() {
    return content;
  }

  @Override
  public void setContent(Map<String, ColumnDefinition> columns) {
    if (content == null) {
      content = columns;
    } else
    // Adding in only the new ColumnDefinition
    {
      columns.keySet().stream().filter(
          column -> !content.containsKey(column))
          .forEachOrdered(column -> content.put(column, columns.get(column)
          ));
    }
  }

  @Override
  public String getName() {
    return name;
  }

  public ContentModel setName(String name) {
    this.name = name;
    return this;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  @Override
  public String getFortressUser() {
    return fortressUser;
  }

  @Override
  public DocumentTypeInputBean getDocumentType() {
    return documentType;
  }

  @Override
  public Boolean isArchiveTags() {
    return archiveTags;
  }

  public void setArchiveTags(boolean archiveTags) {
    this.archiveTags = archiveTags;
  }

  @Override
  public String getEvent() {
    return event;
  }

  public void setEvent(String event) {
    this.event = event;
  }

  @Override
  public ContentModel setFortress(FortressInputBean fortress) {
    this.fortress = fortress;
    return this;
  }

  @Override
  public void setDocumentName(String documentName) {
    this.documentType = new DocumentTypeInputBean(documentName);
  }

  @Override
  public String getSegmentExpression() {
    return segmentExpression;
  }

  public void setSegmentExpression(String segmentExpression) {
    this.segmentExpression = segmentExpression;
  }

  @Override
  public ContentModel setDocumentType(DocumentTypeInputBean documentType) {
    this.documentType = documentType;
    return this;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  // Entity properties
  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }

  @Override
  public String getCondition() {
    return condition;
  }

  public void setCondition(String condition) {
    this.condition = condition;
  }

  @Override
  public Boolean isEmptyIgnored() {
    return emptyIgnored;
  }

  public Boolean isTrackSuppressed() {
    return trackSuppressed;
  }

  public ContentModel setTrackSuppressed(Boolean trackSuppressed) {
    this.trackSuppressed = trackSuppressed;
    return this;
  }

  public Boolean isSearchSuppressed() {
    return searchSuppressed;
  }

  public ContentModel setSearchSuppressed(Boolean searchSuppressed) {
    this.searchSuppressed = searchSuppressed;
    return this;
  }

  @Override
  public ContentModel setTagModel(boolean tagModel) {
    this.tagModel = tagModel;
    return this;
  }

  @Override
  public boolean isTagModel() {
    return documentType == null && fortress == null || tagModel;
  }

  public ContentModel setEmptyIgnored(boolean emptyIgnored) {
    this.emptyIgnored = emptyIgnored;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ContentModelHandler)) {
      return false;
    }

    ContentModelHandler that = (ContentModelHandler) o;

    if (name != null ? !name.equals(that.name) : that.name != null) {
      return false;
    }
    if (entityOnly != null ? !entityOnly.equals(that.entityOnly) : that.entityOnly != null) {
      return false;
    }
    if (archiveTags != null ? !archiveTags.equals(that.archiveTags) : that.archiveTags != null) {
      return false;
    }
    if (event != null ? !event.equals(that.event) : that.event != null) {
      return false;
    }
    if (emptyIgnored != null ? !emptyIgnored.equals(that.emptyIgnored) : that.emptyIgnored != null) {
      return false;
    }
    if (segmentExpression != null ? !segmentExpression.equals(that.segmentExpression) : that.segmentExpression != null) {
      return false;
    }
    if (handler != null ? !handler.equals(that.handler) : that.handler != null) {
      return false;
    }
    return condition != null ? condition.equals(that.condition) : that.condition == null;

  }

  @Override
  public int hashCode() {
    int result = (name != null ? name.hashCode() : 0);
    result = 31 * result + (entityOnly != null ? entityOnly.hashCode() : 0);
    result = 31 * result + (archiveTags != null ? archiveTags.hashCode() : 0);
    result = 31 * result + (event != null ? event.hashCode() : 0);
    result = 31 * result + (emptyIgnored != null ? emptyIgnored.hashCode() : 0);
    result = 31 * result + (segmentExpression != null ? segmentExpression.hashCode() : 0);
    result = 31 * result + (handler != null ? handler.hashCode() : 0);
    result = 31 * result + (condition != null ? condition.hashCode() : 0);
    return result;
  }
}
