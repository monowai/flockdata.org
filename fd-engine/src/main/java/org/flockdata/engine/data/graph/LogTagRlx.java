/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

package org.flockdata.engine.data.graph;

import org.flockdata.data.*;
import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

import java.util.Map;

/**
 * A relationship that once existed between and Entity and a Tag
 * @author mholdsworth
 * @since 4/04/2015
 */
@RelationshipEntity(type = "ARCHIVED_RLX")
public class LogTagRlx extends AbstractEntityTag {
    protected DynamicProperties properties = new DynamicPropertiesContainer();
    @GraphId
    private Long id = null;
    @EndNode
    @Fetch
    private Log log =null;
    @StartNode
    @Fetch
    private TagNode tag = null;
    private String relationship;
    private Boolean reversed;
    private Boolean geoRelationship;

    LogTagRlx(){}

    public LogTagRlx(EntityTag entityTag, Log log, String name) {
        this();
        this.relationship = name;
        this.properties = new DynamicPropertiesContainer(entityTag.getProperties());
        this.reversed = entityTag.isReversed();
        this.tag = (TagNode)entityTag.getTag();
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
    public boolean isGeoRelationship() {
        return geoRelationship;
    }


    public Object getProperty(String key) {
        if (properties == null)
            return null;
        return properties.getProperty(key);
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties.asMap();
    }

}
