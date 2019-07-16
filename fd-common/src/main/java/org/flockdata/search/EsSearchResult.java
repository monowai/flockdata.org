/*
 *  Copyright 2012-2017 the original author or authors.
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

package org.flockdata.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashMap;
import java.util.Map;
import org.flockdata.track.bean.SearchChange;

/**
 * Object to tie the keys between fd-engine and fd-search so that fd-engine can keep the document up-to-date
 *
 * @author mholdsworth
 * @tag Search, Entity, Contract
 * @since 13/07/2013
 */
public class EsSearchResult implements SearchResult {


    private String key, fortress, searchKey, documentType;
    private String indexName; // Store Index
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long logId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long entityId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> data;
    private String code;
    private String createdBy;
    private Long fdTimestamp;
    private String name;
    private String lastUser;
    private String description;
    private String event;
    private Long dateCreated;
    private Long lastUpdate;
    private long whenCreated;

    protected EsSearchResult() {
    }

    public EsSearchResult(SearchChange thisChange) {
        this();
        this.entityId = thisChange.getId();
        this.fortress = thisChange.getFortressName();
        this.searchKey = thisChange.getSearchKey();
        this.documentType = thisChange.getDocumentType();
        this.key = thisChange.getKey();
        this.indexName = thisChange.getIndexName();

        setLogId(thisChange.getLogId());
        setEntityId(thisChange.getId());


    }

    public EsSearchResult(
        String searchKey,
        String key,
        String fortress,
        String event,
        String type,
        String lastUser,
        String lastUpdate,
        String whenCreated,
        String fdTimestamp) {
        this.key = key;
        this.documentType = type;
        this.searchKey = searchKey;
//        this.fragments = fragments;
        this.event = event;
        this.fortress = fortress;
        this.lastUser = lastUser;
        if (whenCreated != null) {
            this.whenCreated = Long.decode(whenCreated);
        }

        if (lastUpdate != null && !lastUpdate.equals(whenCreated)) {
            this.lastUpdate = Long.decode(lastUpdate);
        }

        if (fdTimestamp != null) {
            this.fdTimestamp = Long.decode(fdTimestamp);
        }

    }

    @Override
    public String getLastUser() {
        return lastUser;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getEvent() {
        return event;
    }

    @Override
    public Long getLastUpdate() {
        return lastUpdate;
    }

    @Override
    public long getWhenCreated() {
        return whenCreated;
    }

    /**
     * GUID for the key
     *
     * @return string
     */
    @Override
    public String getKey() {
        return key;
    }

    /**
     * name of the fortress that owns the key
     *
     * @return string
     */
    @Override
    public String getFortress() {
        return fortress;
    }

    @Override
    public String getIndexName() {
        return indexName;
    }

    /**
     * GUID for the search document
     *
     * @return string
     */
    @Override
    public String getSearchKey() {
        return searchKey;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    /**
     * useful for external caller to know what type of document was indexed
     *
     * @return type of Entity
     */
    @Override
    public String getDocumentType() {
        return documentType;
    }

    @Override
    public String toString() {
        return "SearchResult{" +
            "entityId='" + entityId + '\'' +
            ", key='" + key + '\'' +
            ", logId='" + logId + '\'' +
            ", fortress='" + fortress + '\'' +
            ", documentType='" + documentType + '\'' +
            '}';
    }

    @Override
    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    @Override
    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

//    public Map<String, String[]> getFragments() {
//        return fragments;
//    }

    @Override
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public Long getFdTimestamp() {
        return fdTimestamp;
    }

    public void addFieldValue(String field, Object value) {
        if (this.data == null) {
            data = new HashMap<>();
        }
        if (field.contains(SearchSchema.DATA)) {
            field = field.substring(SearchSchema.DATA.length() + 1);
        }
        this.data.put(field, value);
    }

    @Override
    public Map<String, Object> getData() {
        return data;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
