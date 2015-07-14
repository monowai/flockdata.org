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

package org.flockdata.track;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.registration.model.Tag;
import org.flockdata.track.model.Alias;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Unlike the fd-engine TagNode, this one is designed to work with the node infrastructure
 *
 * Created by mike on 23/06/15.
 */
public class TagNode implements Tag {
    String name;
    String code;
    String key;
    Long id;
    String label;
    private Set<Alias> aliases = new HashSet<>();

    TagNode() {
    }

    TagNode(Node node) {
        this();
        for (String key : node.getPropertyKeys()) {
            switch (key) {
                case "code":
                    this.code = node.getProperty(key).toString();
                    break;
                case "name":
                    this.name = node.getProperty(key).toString();
                    break;
                case "key":
                    this.key = node.getProperty(key).toString();
                    break;
            }

        }
        for (Label label : node.getLabels()) {
            if ( !label.name().equals(Tag.DEFAULT_TAG) && !label.name().equals("_"+Tag.DEFAULT_TAG))
                this.label = label.name();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Object getProperty(String key) {
        return null;
    }

    @Override
    public Map<String, Object> getProperties() {
        return null;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public boolean hasAlias(String theLabel, String code) {
        return false;
    }

    @Override
    public Set<Alias> getAliases() {
        return aliases;
    }

    @Override
    public Tag getLocated() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isDefault() {
        return getLabel() == null || Tag.DEFAULT_TAG.equals(getLabel());
    }

}
