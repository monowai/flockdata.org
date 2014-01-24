/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.registration.repo.neo4j.model;

import com.auditbucket.registration.model.*;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 8:35 PM
 */
@NodeEntity
public class SystemNode implements ISystem {
    @GraphId
    Long Id;

    @RelatedTo(elementClass = SystemUserNode.class, type = "ADMINISTERS", direction = Direction.OUTGOING)
    private
    Set<SystemUser> systemUsers;

    @Indexed(indexName = "systemName")
    private String name;

    public SystemNode() {
    }

    public SystemNode(String name) {
        this.name = name;

    }

    @Override
    public String getName() {
        return name;
    }

    public Long getId() {
        return Id;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Set<SystemUser> getSystemUsers() {
        return systemUsers;
    }


}
