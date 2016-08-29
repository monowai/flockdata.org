/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

package org.flockdata.track.bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.model.DocumentType;
import org.flockdata.model.MetaDocument;
import org.flockdata.track.service.EntityService;

/**
 * User: mike
 * Date: 10/10/14
 * Time: 12:06 PM
 */
public class DocumentTypeInputBean implements MetaDocument{
    private String name;
    private String code;

    private String geoQuery;
    private DocumentType.VERSION versionStrategy = DocumentType.VERSION.FORTRESS;
    private EntityService.TAG_STRUCTURE tagStructure = EntityService.TAG_STRUCTURE.DEFAULT;
    private Boolean searchEnabled; // If null default to fortress
    private Boolean storeEnabled; // If null default to fortress
    private Boolean trackEnabled;

    DocumentTypeInputBean(){}

    public DocumentTypeInputBean(String docName) {
        this();
        if ( docName == null || docName.trim().equals(""))
            throw new IllegalArgumentException("DocumentType name is invalid");
        this.name = docName;
        this.code = docName;
    }

    /**
     * Helps unit testing
     * @param documentResultBean result of a previous request to create
     */
    public DocumentTypeInputBean(DocumentResultBean documentResultBean) {
        this.name =documentResultBean.getName();
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
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

    public DocumentTypeInputBean setGeoQuery(final String geoQuery) {
        this.geoQuery = geoQuery;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public DocumentTypeInputBean getVersionStrategy(final DocumentType.VERSION versionStrategy) {
        this.versionStrategy = versionStrategy;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Boolean isSearchEnabled() {
        return searchEnabled;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Boolean isStoreEnabled() {
        return storeEnabled;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Boolean isTrackEnabled() {
        return trackEnabled;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentTypeInputBean)) return false;

        DocumentTypeInputBean that = (DocumentTypeInputBean) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (code != null ? !code.equals(that.code) : that.code != null) return false;
        if (geoQuery != null ? !geoQuery.equals(that.geoQuery) : that.geoQuery != null) return false;
        if (versionStrategy != that.versionStrategy) return false;
        if (tagStructure != that.tagStructure) return false;
        if (searchEnabled != null ? !searchEnabled.equals(that.searchEnabled) : that.searchEnabled != null)
            return false;
        return storeEnabled != null ? storeEnabled.equals(that.storeEnabled) : that.storeEnabled == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (geoQuery != null ? geoQuery.hashCode() : 0);
        result = 31 * result + (versionStrategy != null ? versionStrategy.hashCode() : 0);
        result = 31 * result + (tagStructure != null ? tagStructure.hashCode() : 0);
        result = 31 * result + (searchEnabled != null ? searchEnabled.hashCode() : 0);
        result = 31 * result + (storeEnabled != null ? storeEnabled.hashCode() : 0);
        return result;
    }
}
