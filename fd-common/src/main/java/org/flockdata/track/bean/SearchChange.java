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
