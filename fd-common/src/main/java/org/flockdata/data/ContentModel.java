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

package org.flockdata.data;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.json.ContentModelDeserializer;

/**
 * @author mholdsworth
 * @since 3/10/2014
 */

@JsonDeserialize(using = ContentModelDeserializer.class)
public interface ContentModel {

  DocumentTypeInputBean getDocumentType();

  ContentModel setDocumentType(DocumentTypeInputBean documentType);

  String getName();

  ContentModel setName(String name);

  Map<String, ColumnDefinition> getContent();

  void setContent(Map<String, ColumnDefinition> columns);

  FortressInputBean getFortress();

  ContentModel setFortress(FortressInputBean fortress);

  String getFortressUser();

  Boolean isEntityOnly();

  Boolean isArchiveTags();

  String getEvent();

  void setEvent(String event);

  ColumnDefinition getColumnDef(String column);

  void setFortressName(String fortressName);

  void setDocumentName(String name);

  String getSegmentExpression();

  void setSegmentExpression(String segmentExpression);

  Map<String, Object> getProperties();

  void setEntityOnly(boolean b);

  void setArchiveTags(boolean archiveTags);

  String getHandler();

  String getCondition();

  Boolean isEmptyIgnored();

  boolean isTagModel();

  ContentModel setTagModel(boolean tagModel);

  String getCode(); // Unique user friendly code for a tag model

  Boolean isTrackSuppressed();

  Boolean isSearchSuppressed();

  ContentModel setTrackSuppressed(Boolean suppressed);

  ContentModel setSearchSuppressed(Boolean suppressed);
}
