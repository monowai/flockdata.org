/*
 *  Copyright 2012-2016 the original author or authors.
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

package org.flockdata.profile;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.profile.model.ImportFile;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.transform.ColumnDefinition;

import java.util.Map;

/**
 *
 * Contains logic that can be used to parse raw incoming data in to the ContentModel
 *
 * User: mike
 * Date: 28/04/14
 * Time: 8:47 AM
 */
@JsonDeserialize(using = ImportContentModelDeserializer.class)
public class ImportContentModel implements ContentModel, ImportFile {

    // Default fortress name if not otherwise supplied
    private String fortressName = null;
    private String name = null; // User supplied description of this profile
    private FortressInputBean fortress = null;
    // Default document name if not otherwise supplied
    private String documentName;
    private DocumentTypeInputBean documentType;
    private ContentType contentType;
    private DataType tagOrEntity;
    private String handler = null;

    private boolean emptyIgnored;
    private String delimiter = ",";
    private String quoteCharacter = null;
    private boolean header = true;
    private String fortressUser;
    private boolean entityOnly;
    private boolean archiveTags = true;

    private Map<String, ColumnDefinition> content;
    private String entityKey;
    private String event = null;
    private String preParseRowExp=null;
    private Map<String, Object> properties;
    private String segmentExpression;

    private String condition; // an expression that determines if the row will be processed


    public ImportContentModel() {

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

    public void setDocumentName(String documentName) {
        this.documentType = new DocumentTypeInputBean(documentName);
    }

    @Override
    public String getPreParseRowExp() {
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
        if (delimiter.equals("\t"))
            return '\t';
        return delimiter.charAt(0);
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public void setTagOrEntity(DataType tagOrEntity) {
        this.tagOrEntity = tagOrEntity;
    }

    @Override
    public FortressInputBean getFortress() {
        return fortress;
    }

    @Deprecated // use setFortress and provide default properties
    public void setFortressName(String fortressName) {
        this.fortressName = fortressName;
    }

    @Override
    public boolean hasHeader() {
        return header;
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

    public Map<String, ColumnDefinition> getContent() {
        return content;
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
     * @return should we ignore columns with empty values
     */
    public boolean isEmptyIgnored() {
        return emptyIgnored;
    }

    // Entity properties
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * true/null (default) == process the row
     *
     * @return Expression to evaluate. If it evaluates to false, then the row will be skipped
     */
    public String getCondition() {
        return condition;
    }

    @Override
    public String getSegmentExpression() {
        return segmentExpression;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public void setSegmentExpression(String segmentExpression) {
        this.segmentExpression = segmentExpression;
    }

    public DocumentTypeInputBean getDocumentType() {
        return documentType;
    }

    public String getName(){
        return name;
    }

    public ImportContentModel setName(String name){
        this.name = name;
        return this;
    }

    public ImportContentModel setDocumentType(DocumentTypeInputBean documentType) {
        this.documentType = documentType;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImportContentModel)) return false;

        ImportContentModel that = (ImportContentModel) o;

        if (archiveTags != that.archiveTags) return false;
        if (content != null ? !content.equals(that.content) : that.content != null) return false;
        if (entityKey != null ? !entityKey.equals(that.entityKey) : that.entityKey != null) return false;
        if (event != null ? !event.equals(that.event) : that.event != null) return false;
        if (preParseRowExp != null ? !preParseRowExp.equals(that.preParseRowExp) : that.preParseRowExp != null)
            return false;
        if (properties != null ? !properties.equals(that.properties) : that.properties != null) return false;
        if (segmentExpression != null ? !segmentExpression.equals(that.segmentExpression) : that.segmentExpression != null)
            return false;
        return condition != null ? condition.equals(that.condition) : that.condition == null;

    }

    @Override
    public int hashCode() {
        int result = (archiveTags ? 1 : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        result = 31 * result + (entityKey != null ? entityKey.hashCode() : 0);
        result = 31 * result + (event != null ? event.hashCode() : 0);
        result = 31 * result + (preParseRowExp != null ? preParseRowExp.hashCode() : 0);
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        result = 31 * result + (segmentExpression != null ? segmentExpression.hashCode() : 0);
        result = 31 * result + (condition != null ? condition.hashCode() : 0);
        return result;
    }

    public ContentModel setFortress(FortressInputBean fortress) {
        this.fortress = fortress;
        return this;
    }

}
