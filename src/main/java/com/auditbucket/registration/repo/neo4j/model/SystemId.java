package com.auditbucket.registration.repo.neo4j.model;

import com.auditbucket.registration.model.ISystem;
import com.auditbucket.registration.model.ISystemUser;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.Set;

/**
 * User: mike
 * Date: 26/06/13
 * Time: 8:35 PM
 */
@NodeEntity
public class SystemId implements ISystem {
    @GraphId
    Long Id;

    @RelatedTo(elementClass = SystemUser.class, type = "administers", direction = Direction.OUTGOING)
    Set<ISystemUser> systemUsers;

    @Indexed(indexName = "systemName")
    private String name;

    public SystemId() {
    }

    public SystemId(String name) {
        this.name = name;

    }

    @Override
    public String getName() {
        return name;
    }

    public Long getId() {
        return Id;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Set<ISystemUser> getSystemUsers() {
        return systemUsers;
    }


}
