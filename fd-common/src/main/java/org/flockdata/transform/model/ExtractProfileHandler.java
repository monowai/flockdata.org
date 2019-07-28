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
import org.flockdata.data.ContentModel;


/**
 * Handles extraction configurations for converting Mappable objects via a ContentProfile
 * This is the default deserialized Pojo
 *
 * @author mholdsworth
 * @since 28/04/2014
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExtractProfileHandler implements ExtractProfile {

  private String documentName = null;
  private ContentType contentType = null;
  private String handler = null;
  private String delimiter = null;
  private String quoteCharacter = null;
  private Boolean header = true;
  private String preParseRowExp = null;
  private ContentModel contentModel = null;

  private String condition = null; // an expression that determines if the row will be processed


  public ExtractProfileHandler() {
    this.header = true;
    this.contentType = ContentType.CSV;
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

  // Custom java class name to handle "XML" type transformations
  public void setHandler(String handler) {
    this.handler = handler;
  }

  @Override
  public String getPreParseRowExp() {
    return preParseRowExp;
  }

  public void setPreParseRowExp(String preParseRowExp) {
    this.preParseRowExp = preParseRowExp;
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
    if (delimiter == null) {
      return ',';
    }
    if (delimiter.equals("\t")) {
      return '\t';
    }
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
    if (this == o) {
      return true;
    }
    if (!(o instanceof ExtractProfileHandler)) {
      return false;
    }

    ExtractProfileHandler that = (ExtractProfileHandler) o;

    if (preParseRowExp != null ? !preParseRowExp.equals(that.preParseRowExp) : that.preParseRowExp != null) {
      return false;
    }
    return condition != null ? condition.equals(that.condition) : that.condition == null;

  }

  @Override
  public int hashCode() {
    int result = (preParseRowExp != null ? preParseRowExp.hashCode() : 0);
    result = 31 * result + (condition != null ? condition.hashCode() : 0);
    return result;
  }
}
