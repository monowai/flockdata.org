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

package org.flockdata.profile.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.flockdata.profile.ContentModelDeserializer;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.transform.ColumnDefinition;

import java.util.Map;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 2:51 PM
 */

@JsonDeserialize(using =ContentModelDeserializer.class)
public interface ContentModel {

    DocumentTypeInputBean getDocumentType();

    String getName();

    void setContent(Map<String, ColumnDefinition> columns);

    Map<String, ColumnDefinition> getContent();

    FortressInputBean getFortress() ;

    String getFortressUser();

    Boolean isEntityOnly();

    Boolean isArchiveTags();

    String getEvent();

    ColumnDefinition getColumnDef(String column);

    void setFortressName(String fortressName);

    ContentModel setFortress(FortressInputBean fortress);

    void setDocumentName(String name);

    String getSegmentExpression();

    ContentModel setDocumentType(DocumentTypeInputBean documentType);

    Map<String, Object> getProperties();

    void setEntityOnly(boolean b);

    ContentModel setName(String name);

    void setArchiveTags(boolean archiveTags);

    void setSegmentExpression(String segmentExpression);

    void setEvent(String event);

    String getHandler();

    String getCondition();

    Boolean isEmptyIgnored();

    boolean isTagModel();

    ContentModel setTagModel(boolean tagModel);

    String getCode(); // Unique user friendly code for a tag model

    Boolean isTrackSuppressed();

    Boolean isSearchSuppressed();
}
