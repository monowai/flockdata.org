package com.auditbucket.audit.repo.neo4j.dao;

import com.auditbucket.audit.dao.IAuditTagDao;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.ITagValue;
import com.auditbucket.audit.repo.neo4j.AuditTagRepo;
import com.auditbucket.audit.repo.neo4j.model.AuditTagValue;
import com.auditbucket.registration.model.ITag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * User: mike
 * Date: 28/06/13
 * Time: 11:07 AM
 */
@Repository("auditTagDAO")
public class AuditTagDao implements IAuditTagDao {
    @Autowired
    Neo4jTemplate template;

    @Autowired
    AuditTagRepo auditTagRepo;

    @Override
    public ITagValue save(ITag tagName, IAuditHeader header, String tagValue) {
        AuditTagValue atv = new AuditTagValue(tagName, header, tagValue);
        return template.save(atv);

    }

    @Override
    public Set<ITagValue> find(ITag tagName, String tagValue) {
        return auditTagRepo.findTagValues(tagName.getId(), tagValue);
    }
}
