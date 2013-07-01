package com.auditbucket.audit.dao;

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.ITagValue;
import com.auditbucket.registration.model.ITag;

import java.util.Set;

/**
 * User: mike
 * Date: 28/06/13
 * Time: 9:55 AM
 */
public interface IAuditTagDao {
    ITagValue save(ITag tagName, IAuditHeader header, String tagValue);

    Set<ITagValue> find(ITag tagName, String tagValue);

    Set<ITagValue> getAuditTags(IAuditHeader ah);

    void update(Set<ITagValue> modifiedSet);
}
