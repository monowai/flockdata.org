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

import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.profile.model.ExtractProfile;


/**
 *
 * Handles extraction configurations for converting Mappable objects via a ContentProfile
 * This is the default deserialized Pojo
 *
 * User: mike
 * Date: 28/04/14
 * Time: 8:47 AM
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExtractProfileHandler implements ExtractProfile {

    private String documentName=null;
    private ContentType contentType=null;
    private String handler = null;
    private String delimiter = null;
    private String quoteCharacter = null;
    private Boolean header = true;
    private String preParseRowExp=null;
    private ContentModel contentModel = null;

    private String condition =null; // an expression that determines if the row will be processed


    public ExtractProfileHandler() {
        this.header = true;
        this.delimiter =",";
        this.contentType=ContentType.CSV;
    }

    public ExtractProfileHandler(ContentModel contentModel) {
        this();
        this.contentModel = contentModel;
    }

    public ExtractProfileHandler(ContentModel contentModel, boolean hasHeader) {
        this(contentModel);
        this.header = hasHeader;

    }

    public ExtractProfileHandler(ContentModel contentModel, String delimiter) {
        this(contentModel);
        this.delimiter = delimiter;
    }

    public ExtractProfile setHeader(boolean header) {
        this.header = header;
        return this;
    }

    @Override
    public String toString() {
        return "ImportProfile{" +
                "documentName='" + documentName + '\'' +
                ", contentType=" + contentType +
                ", handler='" + handler + '\'' +
                ", delimiter=" + delimiter +
                '}';
    }

    @Override
    public String getHandler() {
        return handler;
    }

    @Override
    public String getPreParseRowExp() {
        return preParseRowExp;
    }

    @Override
    public String getQuoteCharacter() {
        return quoteCharacter;
    }

    public ExtractProfile setQuoteCharacter(String quoteCharacter) {
        this.quoteCharacter = quoteCharacter;
        return this;
    }

    @Override
    public ContentType getContentType() {
        return contentType;
    }

    public ExtractProfile setContentType(ContentType contentType) {
        this.contentType = contentType;
        return this;
    }

    @Override
    public char getDelimiter() {
        if ( delimiter ==null )
            return ',';
        if (delimiter.equals("\t"))
            return '\t';
        return delimiter.charAt(0);
    }

    public ExtractProfile setDelimiter(String delimiter) {
        this.delimiter = delimiter;
        return this;
    }

    @Override
    public Boolean hasHeader() {
        return header;
    }

    public void setPreParseRowExp(String preParseRowExp) {
        this.preParseRowExp = preParseRowExp;
    }

    public ExtractProfile setCondition(String condition) {
        this.condition = condition;
        return this;
    }

    public ContentModel getContentModel() {
        return contentModel;
    }

    public ExtractProfile setContentModel(ContentModel contentModel) {
        this.contentModel = contentModel;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExtractProfileHandler)) return false;

        ExtractProfileHandler that = (ExtractProfileHandler) o;

        if (preParseRowExp != null ? !preParseRowExp.equals(that.preParseRowExp) : that.preParseRowExp != null)
            return false;
        return condition != null ? condition.equals(that.condition) : that.condition == null;

    }

    @Override
    public int hashCode() {
        int result =  (preParseRowExp != null ? preParseRowExp.hashCode() : 0);
        result = 31 * result + (condition != null ? condition.hashCode() : 0);
        return result;
    }

    // Custom java class name to handle "XML" type transformations
    public void setHandler(String handler) {
        this.handler = handler;
    }
}