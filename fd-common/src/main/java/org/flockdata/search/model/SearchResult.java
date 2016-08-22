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

package org.flockdata.search.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.track.bean.SearchChange;

import java.util.HashMap;
import java.util.Map;

/**
 * Object to tie the keys between fd-engine and fd-search so that fd-engine can keep the document up-to-date
 * <p/>
 * User: Mike Holdsworth
 * Since: 13/07/13
 */
public class SearchResult {


    private String key, fortress, searchKey, documentType;
    private String indexName; // Store Index
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long logId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long entityId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, String[]> fragments;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> data;
    private String code;
    private String createdBy;
    private Long fdTimestamp;
    private String name;

    public String getLastUser() {
        return lastUser;
    }

    public String getDescription() {
        return description;
    }

    public String getEvent() {
        return event;
    }

    public Long getLastUpdate() {
        return lastUpdate;
    }

    public long getWhenCreated() {
        return whenCreated;
    }

    private String lastUser;
    private String description;
    private String event;
    private Long dateCreated;
    private Long lastUpdate;
    private long whenCreated;

    protected SearchResult() {
    }

    public SearchResult(SearchChange thisChange) {
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

    public SearchResult(
                        String searchKey,
                        String key,
                        String fortress,
                        String event,
                        String type,
                        String lastUser,
                        String lastUpdate,
                        String whenCreated,
                        String fdTimestamp,
                        Map<String, String[]> fragments) {
        this.key = key;
        this.documentType = type;
        this.searchKey = searchKey;
        this.fragments = fragments;
        this.event = event;
        this.fortress = fortress;
        this.lastUser = lastUser;
        if ( whenCreated !=null )
            this.whenCreated= Long.decode(whenCreated);

        if ( lastUpdate != null && !lastUpdate.equals(whenCreated) )
            this.lastUpdate = Long.decode(lastUpdate);

        if ( fdTimestamp !=null )
            this.fdTimestamp = Long.decode(fdTimestamp);

    }

    /**
     * GUID for the key
     *
     * @return string
     */
    public String getKey() {
        return key;
    }

    /**
     * name of the fortress that owns the key
     *
     * @return string
     */
    public String getFortress() {
        return fortress;
    }

    public String getIndexName() {
        return indexName;
    }

    /**
     * GUID for the search document
     *
     * @return string
     */
    public String getSearchKey() {
        return searchKey;
    }

    /**
     * useful for external caller to know what type of document was indexed
     *
     * @return
     */
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

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    public Map<String, String[]> getFragments() {
        return fragments;
    }

    public String getCode() {
        return code;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Long getFdTimestamp() {
        return fdTimestamp;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void addFieldValue(String field, Object value) {
        if ( this.data == null )
            data = new HashMap<>();
        if ( field.contains(SearchSchema.DATA))
            field = field.substring(SearchSchema.DATA.length()+1);
        this.data.put(field, value);
    }

    public Map<String,Object> getData(){
        return data;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
