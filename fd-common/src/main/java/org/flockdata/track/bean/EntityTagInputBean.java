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

import org.flockdata.helper.FlockException;
import org.flockdata.registration.model.Tag;

import javax.validation.constraints.NotNull;

/**
 * Associates a Tag with an Entity
 * <p/>
 * User: Mike Holdsworth
 * Date: 28/06/13
 * Time: 9:58 PM
 */
public class EntityTagInputBean {
    @NotNull
    private String tagCode;
    @NotNull
    private String metaKey;
    @NotNull
    private String type;
    private String index;
    private boolean since;

    private EntityTagInputBean() {
    }

    /**
     * @param metaKey          metaKey for an existing entity
     * @param tagCode          name of an existing tag
     * @param relationshipName relationship name to create
     */
    public EntityTagInputBean(String metaKey, String tagCode, String relationshipName) throws FlockException {
        this();
//        if ( tagCode == null )
//            throw new FlockException("tagCode cannot be null");
        this.metaKey = metaKey;
        this.tagCode = tagCode;
        if (relationshipName == null)
            this.type = "general";
        else
            this.type = relationshipName;
    }

    public String getMetaKey() {
        return metaKey;
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

    /**
     * When connecting the tag to the entity, tell FD to record a timestamp as a user defined property
     * @param since yes/no
     * @return this
     */
    public EntityTagInputBean setSince(boolean since) {
        this.since = since;
        return this;
    }

    public boolean isSince() {
        return since;
    }
}
