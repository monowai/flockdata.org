package com.auditbucket.audit.service;

import com.auditbucket.audit.bean.AuditTagInputBean;
import com.auditbucket.audit.dao.IAuditTagDao;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.ITagValue;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.ITag;
import com.auditbucket.registration.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * User: mike
 * Date: 27/06/13
 * Time: 5:07 PM
 */
@Service
public class AuditTagService {
    @Autowired
    AuditService auditService;

    @Autowired
    TagService tagService;

    @Autowired
    SecurityHelper securityHelper;

    @Autowired
    IAuditTagDao auditTagDao;

    public void processTag(AuditTagInputBean tagInput) {
        //ICompany company = securityHelper.getCompany();
        String tagValue = tagInput.getValue();
        Set<ITagValue> existing = findTagValues(tagInput.getTagName(), tagInput.getValue());
        if (existing != null && existing.size() == 1)
            // We already have this tagged so get out of here
            return;

        ITag tag = tagService.processTag(new TagInputBean(tagInput.getTagName()));
        IAuditHeader header = auditService.getHeader(tagInput.getAuditKey());
        auditTagDao.save(tag, header, tagValue);
    }

    public Set<ITagValue> findTagValues(String tagName, String tagValue) {
        ITag tag = tagService.findTag(tagName);
        if (tag == null)
            return null;
        return auditTagDao.find(tag, tagValue);
    }
}
