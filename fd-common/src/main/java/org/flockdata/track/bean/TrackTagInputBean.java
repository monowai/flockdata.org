/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

import org.flockdata.registration.model.Tag;

import javax.validation.constraints.NotNull;

/**
 * Associates a Tag with an Entity
 * <p/>
 * User: Mike Holdsworth
 * Date: 28/06/13
 * Time: 9:58 PM
 */
public class TrackTagInputBean {
    @NotNull
    private String tagName;
    @NotNull
    private String metaKey;
    @NotNull
    private String type;
    private String index;

    private TrackTagInputBean() {
    }

    /**
     * @param metaKey          metaKey for an existing entity
     * @param tagName          name of an existing tag
     * @param relationshipName relationship name to create
     */
    public TrackTagInputBean(String metaKey, String tagName, String relationshipName) {
        this();
        this.metaKey = metaKey;
        this.tagName = tagName;
        if (relationshipName == null)
            this.type = "general";
        else
            this.type = relationshipName;
    }

    public String getMetaKey() {
        return metaKey;
    }

    public String getTagName() {
        return tagName;
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

}
