/*
 * Copyright (c) 2012-2015 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.track.bean;

import org.flockdata.model.EntityTag;
import org.flockdata.search.model.EntitySearchChange;
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
    String getMetaKey();

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
}
