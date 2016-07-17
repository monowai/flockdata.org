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

import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

import java.util.Map;

/**
 * A relationship that once existed between and Entity and a Tag
 * Created by mike on 4/04/15.
 */
@RelationshipEntity(type = "ARCHIVED_RLX")
public class LogTag extends AbstractEntityTag {
    @GraphId
    private Long id = null;

    @EndNode
    @Fetch
    private Log log =null;

    @StartNode
    @Fetch
    private Tag tag = null;

    private String relationship;

    private Boolean reversed;

    private Boolean geoRelationship;

    //DynamicProperties properties = new DynamicPropertiesContainer();

    LogTag(){}

    public LogTag(EntityTag entityTag, Log log, String name) {
        this();
        this.relationship = name;
        this.properties = new DynamicPropertiesContainer(entityTag.getProperties());
        this.reversed = entityTag.isReversed();
        this.tag = entityTag.getTag();
        this.log = log;
        this.geoRelationship=entityTag.isGeoRelationship();

    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public Entity getEntity() {
        return null;
    }

    //@Override
    public Log getLog(){
        return log;
    }

    @Override
    public Tag getTag() {
        return tag;
    }

    @Override
    public Map<String, Object> getTagProperties() {
        return properties.asMap();
    }

    @Override
    public Boolean isReversed() {
        return reversed;
    }

    @Override
    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship){
        this.relationship = relationship;
    }

    @Override
    public boolean isGeo() {
        return geoRelationship;
    }
}
