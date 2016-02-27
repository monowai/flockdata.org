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

import org.flockdata.model.EntityTag;
import org.flockdata.search.model.SearchTag;
import org.flockdata.track.service.EntityService;
import org.joda.time.DateTime;

import java.util.*;

/**
 * User: Mike Holdsworth
 * Date: 21/04/13
 * Time: 7:44 PM
 */
public interface SearchChange {
    /**
     * @return search keys unique document identifier
     */
    String getSearchKey();

    EntityKeyBean getParent();

    void setSearchKey(String parent);

    /**
     * primary key of the Entity record that this document belongs to
     *
     * @return GUID
     */
    String getKey();

    SearchChange setData(Map<String, Object> what);

    Map<String, Object> getData();

    HashMap<String, Map<String, ArrayList<SearchTag>>> getTagValues();

    SearchChange setStructuredTags(EntityService.TAG_STRUCTURE tagStructure, Iterable<EntityTag> tagSet);

    /**
     * @return who made this change
     */
    String getWho();

    Long getSysWhen();

    Date getCreatedDate();

    Date getUpdatedDate();

    String getFortressName();

    String getCompanyName();

    String getIndexName();

    /**
     * when this log was created in the Fortress
     *
     * @param date date
     */
    void setWhen(DateTime date);

    void setWho(String name);

    String getDocumentType();

    String getCode();

    String getEvent();

    void setSysWhen(Long sysWhen);

    void setLogId(Long id);

    Long getLogId();

    Long getEntityId();

    SearchChange setDescription(String description);

    String getDescription();

    /**
     * Hint to determine if a reply from the search service is expected
     * by the caller
     * <p/>
     * default to true
     */
    void setReplyRequired(boolean required);

    boolean isReplyRequired();

    /**
     * Forces the search engine to ignore date checks and force an update of the document.
     * Usually in response to a cancellation in fd-engine
     *
     */
    boolean isForceReindex();

    /**
     *
     * @return if the searchKey should be removed
     */
    Boolean isDelete();

    void setName(String name);

    void setAttachment(String attachment);

    boolean hasAttachment();

    String getAttachment();

    String getFileName();

    String getContentType();

    Map<String,Object> getProps();

    EntityService.TAG_STRUCTURE getTagStructure();

    SearchChange setStructuredTags(ArrayList<EntityTag> tagsB);

    SearchChange setParent(EntityKeyBean parent);

    String getSegment();

    SearchChange addEntityLinks(Collection<EntityKeyBean> inboundEntities);

    Collection<EntityKeyBean> getEntityLinks();

    String getName();
}
