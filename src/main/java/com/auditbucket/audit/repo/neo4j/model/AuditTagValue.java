package com.auditbucket.audit.repo.neo4j.model;

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.ITagValue;
import com.auditbucket.registration.model.ITag;
import com.auditbucket.registration.repo.neo4j.model.Tag;
import org.springframework.data.neo4j.annotation.*;

/**
 * User: mike
 * Date: 26/06/13
 * Time: 12:59 PM
 */

@RelationshipEntity(type = "tagValue")
public class AuditTagValue implements ITagValue {
    @GraphId
    Long id;

    @StartNode
    @Fetch
    Tag tag;

    @EndNode

    AuditHeader auditHeader;

    @Indexed(indexName = "tagValue")
    String tagValue;

    protected AuditTagValue() {
    }

    public AuditTagValue(ITag tag, IAuditHeader header, String tagValue) {
        this.tag = (Tag) tag;
        this.auditHeader = (AuditHeader) header;
        this.tagValue = tagValue;
    }

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

    public void setTagValue(String tagValue) {
        this.tagValue = tagValue;
    }
}
