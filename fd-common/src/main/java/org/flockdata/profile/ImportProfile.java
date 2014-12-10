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

package org.flockdata.profile;

import org.flockdata.profile.model.Mappable;
import org.flockdata.profile.model.ProfileConfiguration;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.FdReader;
import org.flockdata.transform.TagMapper;
import org.flockdata.transform.csv.CsvEntityMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * See also ImportParamsDeserializer for mapping logic
 * User: mike
 * Date: 28/04/14
 * Time: 8:47 AM
 */
@JsonDeserialize(using = ImportProfileDeserializer.class)
public class ImportProfile implements ProfileConfiguration {

    // Default fortress name if not otherwise supplied
    private String fortressName = null;
    // Default document name if not otherwise supplied
    private String documentName;
    private ContentType contentType;
    private DataType tagOrEntity;
    private String clazz = null;
    private String staticDataClazz;
    private char delimiter = ',';
    private boolean header = true;
    private String fortressUser;
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(ImportProfile.class);
    private boolean entityOnly;
    private boolean archiveTags = true;

    private Map<String, ColumnDefinition> content;
    private FdReader staticDataResolver;
    private String entityKey;
    private String event = null;

    public ImportProfile() {

    }

//    public ImportProfile(IStaticDataResolver restClient) {
//        this.restClient = restClient;
//    }
//
//    public ImportProfile(String clazz, IStaticDataResolver restClient) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
//        this(restClient);
//        this.clazz = clazz;
//        this.contentType = ((Mappable) Class.forName(getClazz()).newInstance()).getImporter();
//
//    }

    public void setHeader(boolean header) {
        this.header = header;
    }

    public void setFortressUser(String fortressUser) {
        this.fortressUser = fortressUser;
    }

    public void setContent(Map<String, ColumnDefinition> columns) {
        this.content = columns;
    }

    @Override
    public String toString() {
        return "ImportProfile{" +
                "documentName='" + documentName + '\'' +
                ", contentType=" + contentType +
                ", tagOrEntity='" + tagOrEntity + '\'' +
                ", clazz='" + clazz + '\'' +
                ", delimiter=" + delimiter +
                '}';
    }

    @Override
    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    @Override
    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    @Override
    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    @Override
    public char getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(char delimiter) {
        this.delimiter = delimiter;
    }

    @Override
    public DataType getTagOrEntity() {
        return tagOrEntity;
    }

    public void setTagOrEntity(DataType tagOrEntity) {
        this.tagOrEntity = tagOrEntity;
    }

    @Override
    public String getFortressName() {
        return fortressName;
    }

    public void setFortressName(String fortressName) {
        this.fortressName = fortressName;
    }

    @Override
    public boolean hasHeader() {
        return header;
    }

    @Override
    public Mappable getMappable() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Mappable mappable = null;

        if (!(clazz == null || clazz.equals("")))
            mappable = (Mappable) Class.forName(getClazz()).newInstance();
        else if (getTagOrEntity()== DataType.ENTITY) {
            mappable = CsvEntityMapper.newInstance(this);
        } else if (getTagOrEntity()== DataType.TAG) {
            mappable = TagMapper.newInstance(this);
        } else
            logger.error("Unable to determine the implementing handler");


        return mappable;

    }

    @Override
    public String getFortressUser() {
        return fortressUser;
    }

    @Override
    public boolean isEntityOnly() {
        return entityOnly;
    }

    public void setEntityOnly(boolean entityOnly) {
        this.entityOnly = entityOnly;
    }

    public ColumnDefinition getColumnDef(String column) {
        if (content == null)
            return null;
        return content.get(column);
    }

    public void setStaticDataResolver(FdReader staticDataResolver) {
        this.staticDataResolver = staticDataResolver;
    }

    public void setEntityKey(String entityKey) {
        this.entityKey = entityKey;
    }

    @Override
    public String getEntityKey() {
        return entityKey;
    }

    public Map<String, ColumnDefinition> getContent() {
        return content;
    }

    @Override
    public Collection<String> getStrategyCols() {
        Map<String, ColumnDefinition> columns = getContent();

        ArrayList<String> strategyColumns = new ArrayList<>();
        if (columns == null )
            return strategyColumns;
        for (String column : columns.keySet()) {
            String strategy = columns.get(column).getStrategy();
            if (strategy != null)
                strategyColumns.add(column);
        }
        return strategyColumns;
    }


    @Override
    public boolean isArchiveTags() {
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

    public void setStaticDataClazz(String staticDataClazz) {
        this.staticDataClazz = staticDataClazz;
    }

    public String getStaticDataClazz() {
        return staticDataClazz;
    }
}
