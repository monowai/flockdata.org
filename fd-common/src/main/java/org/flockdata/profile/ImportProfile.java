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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.flockdata.profile.model.Mappable;
import org.flockdata.profile.model.ProfileConfiguration;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.csv.CsvEntityMapper;
import org.flockdata.transform.tags.TagMapper;
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
    private String handler = null;

    private boolean emptyIgnored;
    private String delimiter = ",";
    private String quoteCharacter = null;
    private boolean header = true;
    private String fortressUser;
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(ImportProfile.class);
    private boolean entityOnly;
    private boolean archiveTags = true;

    private Map<String, ColumnDefinition> content;
    private String entityKey;
    private String event = null;
    private String preParseRowExp;

    public ImportProfile() {

    }

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
                ", handler='" + handler + '\'' +
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
    public String getPreParseRowExp() {
        if (preParseRowExp != null && preParseRowExp.equalsIgnoreCase("null"))
            return null;
        return preParseRowExp;
    }

    @Override
    public String getQuoteCharacter() {
        return quoteCharacter;
    }

    public void setQuoteCharacter(String quoteCharacter) {
        this.quoteCharacter = quoteCharacter;
    }

    @Override
    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    @Override
    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    @Override
    public char getDelimiter() {
        if ( delimiter.equals("\t"))
                return '\t';
        return delimiter.charAt(0);
    }

    public void setDelimiter(String delimiter) {
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

        if (!(handler == null || handler.equals("")))
            mappable = (Mappable) Class.forName(getHandler()).newInstance();
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

    public void setEmptyIgnored(boolean emptyIgnored) {
        this.emptyIgnored = emptyIgnored;
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

    public void setPreParseRowExp(String preParseRowExp) {
        this.preParseRowExp = preParseRowExp;
    }

    /**
     *
     * @return should we ignore columns with empty values
     */
    public boolean isEmptyIgnored() {
        return emptyIgnored;
    }


}
