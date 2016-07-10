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
