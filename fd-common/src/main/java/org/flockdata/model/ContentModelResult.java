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

package org.flockdata.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.data.ContentModel;
import org.flockdata.data.Model;

/**
 * @author mholdsworth
 * @since 14/04/2016
 */
public class ContentModelResult {

  private String key;
  private String code;
  private String name;
  private String documentType;
  private String fortress;
  private ContentModel contentModel;

  ContentModelResult() {

  }

  public ContentModelResult(Model model) {
    this();
    this.key = model.getKey();
    this.name = model.getName();
    this.code = model.getCode();
    if (model.getFortress() != null) {
      this.fortress = model.getFortress().getName();
    } else if (model.getFortressName() != null) {
      this.fortress = model.getFortressName();
    } else {
      this.fortress = "Tag";
    }

    if (model.getDocument() == null) {
      if (model.getCode() != null) {
        this.code = model.getCode();
        this.documentType = model.getCode();
      } else {
        this.documentType = model.getDocumentName();
      }
    } else {
      this.documentType = model.getDocument().getName();

    }

  }

  public String getKey() {
    return key;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String getDocumentType() {
    return documentType;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String getFortress() {
    return fortress;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public ContentModel getContentModel() {
    return contentModel;
  }

  public void setContentModel(ContentModel contentModel) {
    this.contentModel = contentModel;
  }

  @Override
  public String toString() {
    return "ContentModelResult{" +
        "code='" + code + '\'' +
        ", name='" + name + '\'' +
        ", documentType='" + documentType + '\'' +
        ", fortress='" + fortress + '\'' +
        ", key='" + key + '\'' +
        '}';
  }
}
