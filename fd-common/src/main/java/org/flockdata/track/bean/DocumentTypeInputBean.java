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

package org.flockdata.track.bean;

import org.flockdata.model.DocumentType;
import org.flockdata.track.service.EntityService;

/**
 * User: mike
 * Date: 10/10/14
 * Time: 12:06 PM
 */
public class DocumentTypeInputBean {
    private String name;
    private String code;

    private String geoQuery;
    private DocumentType.VERSION versionStrategy = DocumentType.VERSION.FORTRESS;
    private EntityService.TAG_STRUCTURE tagStructure = EntityService.TAG_STRUCTURE.DEFAULT;
    private Boolean searchActive; // If null default to fortress
    private Boolean storeActive; // If null default to fortress

    DocumentTypeInputBean(){}

    public DocumentTypeInputBean(String docName) {
        this();
        if ( docName == null || docName.trim().equals(""))
            throw new IllegalArgumentException("DocumentType name is invalid");
        this.name = docName;
        this.code = docName;
    }

    public String getName() {
        return name;
    }

    public DocumentTypeInputBean setName(String name) {
        this.name = name;
        return this;
    }

    public String getCode() {
        return code;
    }

    public DocumentTypeInputBean setCode(String code) {
        this.code = code;
        return this;
    }

    // MKH - Overrides the default geo query path for this DocumentType. VULNERABLE!
    public String getGeoQuery() {
        // DAT-507
        return geoQuery;
    }

    public DocumentTypeInputBean setVersionStrategy(DocumentType.VERSION versionStrategy) {
        this.versionStrategy = versionStrategy;
        return this;
    }

    public DocumentType.VERSION getVersionStrategy() {
        return versionStrategy;
    }

    public EntityService.TAG_STRUCTURE getTagStructure() {
        return tagStructure;
    }

    public DocumentTypeInputBean getName(final String name) {
        this.name = name;
        return this;
    }

    public DocumentTypeInputBean getCode(final String code) {
        this.code = code;
        return this;
    }

    public DocumentTypeInputBean getGeoQuery(final String geoQuery) {
        this.geoQuery = geoQuery;
        return this;
    }

    public DocumentTypeInputBean getVersionStrategy(final DocumentType.VERSION versionStrategy) {
        this.versionStrategy = versionStrategy;
        return this;
    }

    public Boolean isSearchActive() {
        return searchActive;
    }

    public Boolean isStoreActive() {
        return storeActive;
    }

    public DocumentTypeInputBean setTagStructure(EntityService.TAG_STRUCTURE tagStructure) {
        this.tagStructure = tagStructure;
        return this;
    }

    @Override
    public String toString() {
        return "DocumentTypeInputBean{" +
                "name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", tagStructure=" + tagStructure +
                ", versionStrategy=" + versionStrategy +
                '}';
    }
}
