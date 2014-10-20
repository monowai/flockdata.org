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

package org.flockdata.profile.model;

import org.flockdata.transform.ColumnDefinition;

import java.util.Collection;
import java.util.Map;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 2:51 PM
 */
public interface ProfileConfiguration {
    String getDocumentName();

    ContentType getContentType();

    public void setContent(Map<String, ColumnDefinition> columns);

    public Map<String, ColumnDefinition> getContent();

    String getClazz();

    char getDelimiter();

    String getTagOrEntity();

    String getFortressName();

    boolean hasHeader();

    Mappable getMappable() throws ClassNotFoundException, IllegalAccessException, InstantiationException;

    String getFortressUser();

    boolean isEntityOnly();

    String getEntityKey();

    Collection<String> getStrategyCols();

    boolean isArchiveTags();

    String getEvent();

    String getStaticDataClazz();

    ColumnDefinition getColumnDef(String column);

    void setFortressName(String fortressName);

    void setDocumentName(String name);

    enum ContentType {CSV, JSON, XML}

    enum DataType {TRACK, TAG}
}