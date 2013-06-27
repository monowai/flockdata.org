package com.auditbucket.audit.repo.neo4j.model;

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.ITagValue;
import com.auditbucket.registration.model.ITag;
import com.auditbucket.registration.repo.neo4j.model.Tag;
import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.StartNode;

/**
 * User: mike
 * Date: 26/06/13
 * Time: 12:59 PM
 */

@RelationshipEntity(type = "tagValue")
public class AuditTagValue implements ITagValue {
    @StartNode
    Tag tag;

    @EndNode
    AuditHeader auditHeader;

    String tagValue;

    @Override
    public ITag getTag() {
        return tag;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public IAuditHeader getHeader() {
        return auditHeader;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getTagValue() {
        return tagValue;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
