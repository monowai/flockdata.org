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
import org.flockdata.model.Model;
import org.flockdata.profile.model.ContentModel;

/**
 * Created by mike on 14/04/16.
 */
public class ContentModelResult {

    private String key;
    private String code;
    private String name;
    private String documentType;
    private String fortress ;
    private ContentModel contentModel;

    ContentModelResult() {

    }

    public ContentModelResult(Model model){
        this();
        this.key = model.getKey();
        this.name = model.getName();
        this.code = model.getCode();
        if ( model.getFortress()!=null)
            this.fortress = model.getFortress().getName();
        this.documentType = model.getDocument().getName();

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

    public void setContentModel(ContentModel contentModel) {
        this.contentModel = contentModel;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public ContentModel getContentModel() {
        return contentModel;
    }
}
