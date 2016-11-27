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

import org.flockdata.helper.FlockException;
import org.flockdata.model.Tag;


/**
 * Associates a Tag with an Entity
 * <p/>
 * @author mholdsworth
 * @since 28/06/2013
 * @tag Payload, Tag, Entity
 */
public class EntityTagInputBean{
    private String tagCode;
    private String key;
    private String type;
    private String index;
    private boolean since;
    private Boolean geoRlx;
    private String tagKeyPrefix;

    private EntityTagInputBean() {
    }

    /**
     * @param key          key for an existing entity
     * @param tagCode          name of an existing tag
     * @param relationshipName relationship name to create
     */
    public EntityTagInputBean(String key, String tagCode, String relationshipName) throws FlockException {
        this();
//        if ( tagCode == null )
//            throw new FlockException("tagCode cannot be null");
        this.key = key;
        this.tagCode = tagCode;
        if (relationshipName == null)
            this.type = "general";
        else
            this.type = relationshipName;
    }

    public String getKey() {
        return key;
    }

    public String getTagCode() {
        return tagCode;
    }

    /**
     * Type of relationship
     *
     * @return relationship name
     */
    public String getType() {
        return type;
    }

    public String getIndex() {
        if (index==null ||"".equals(index))
            return Tag.DEFAULT;
        else
            return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public boolean isSince() {
        return since;
    }

    /**
     * When connecting the tag to the entity, tell FD to record a timestamp as a user defined property
     * @param since yes/no
     * @return this
     */
    public EntityTagInputBean setSince(boolean since) {
        this.since = since;
        return this;
    }

    public String getTagKeyPrefix() {
        return tagKeyPrefix;
    }

    public void setTagKeyPrefix(String tagKeyPrefix) {
        this.tagKeyPrefix = tagKeyPrefix;
    }

    public Boolean isGeoRlx() {
        return geoRlx;
    }
}
