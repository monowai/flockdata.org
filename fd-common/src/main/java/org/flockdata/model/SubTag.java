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

package org.flockdata.model;

import java.util.Map;

/**
 * @author mholdsworth
 * @since 22/08/2015
 */
public class SubTag extends AbstractEntityTag {
    Long id ;
    Tag tag;
    String relationship;

    public SubTag() {}

    public SubTag(Tag subTag, String label) {
        this.tag = subTag;
        this.relationship = label;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public Entity getEntity() {
        return null;
    }

    @Override
    public Tag getTag() {
        return tag;
    }

    @Override
    public String getRelationship() {
        return relationship;
    }

    @Override
    public void setRelationship(String name) {
        this.relationship = name;
    }

    @Override
    public boolean isGeo() {
        return false;
    }

    @Override
    public Map<String, Object> getTagProperties() {
        return null;
    }

    @Override
    public Boolean isReversed() {
        return false;
    }
}
