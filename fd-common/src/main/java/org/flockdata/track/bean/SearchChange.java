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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.track.service.EntityService;

import java.util.Map;

/**
 * User: Mike Holdsworth
 * Date: 21/04/13
 * Time: 7:44 PM
 */
public interface SearchChange<T> {

    @JsonIgnore
    boolean isType(Type type);

    enum Type { ENTITY, TAG }

    /**
     *
     * @return String representation of the Type of searchChange that this represents (entity/tag)
     */
    String getType();

    /**
     * @return unique key identifier for the document in the search service
     */
    String getSearchKey();

    String getName();

    void setName(String name);

    Long getLogId();

    void setSearchKey(String key);

    /**
     * primary key of the Item that this document belongs to
     *
     * @return unique key for a documentType
     */
    String getKey();

    String getIndexName();

    String getFortressName();

    String getDocumentType();

    String getCode();

    /**
     *
     * @return unique identify number in fd-engine
     */
    Long getId();

    T setDescription(String description);

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

    Map<String,Object> getProps();

    EntityService.TAG_STRUCTURE getTagStructure();


    EntityKeyBean getParent();


}
