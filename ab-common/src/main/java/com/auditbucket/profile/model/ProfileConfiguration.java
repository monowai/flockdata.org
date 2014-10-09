package com.auditbucket.profile.model;

import com.auditbucket.transform.ColumnDefinition;

import java.util.Collection;
import java.util.Map;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 2:51 PM
 */
public interface ProfileConfiguration {
    String getDocumentType();

    ContentType getContentType();

    public void setColumns(Map<String, ColumnDefinition> columns);

    public Map<String, ColumnDefinition> getColumns();

    String getClazz();

    char getDelimiter();

    String getTagOrEntity();

    String getFortress();

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

    enum ContentType {CSV, JSON, XML}

    enum DataType {TRACK, TAG}
}
