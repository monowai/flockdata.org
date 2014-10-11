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
