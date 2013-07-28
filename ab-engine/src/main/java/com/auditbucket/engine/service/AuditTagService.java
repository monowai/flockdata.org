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

package com.auditbucket.engine.service;

import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.TagValue;
import com.auditbucket.bean.AuditTagInputBean;
import com.auditbucket.dao.IAuditTagDao;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * User: Mike Holdsworth
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
        //Company company = securityHelper.getCompany();
        String tagValue = tagInput.getValue();
        Set<TagValue> existing = findTagValues(tagInput.getTagName(), tagInput.getValue());
        if (existing != null && existing.size() == 1)
            // We already have this tagged so get out of here
            return;

        Tag tag = tagService.processTag(new TagInputBean(tagInput.getTagName()));
        AuditHeader header = auditService.getHeader(tagInput.getAuditKey());
        auditTagDao.save(tag, header, tagValue);
    }

    public Set<TagValue> findTagValues(String tagName, String tagValue) {
        Tag tag = tagService.findTag(tagName);
        if (tag == null)
            return null;
        return auditTagDao.find(tag, tagValue);
    }

    /**
     * Will associate the supplied userTags with the AuditHeaderNode
     *
     * @param userTags Key/Value pair of tags. TagNode will be created if missing
     * @param ah       Header to associate userTags with
     */
    public void createTagValues(Map<String, Object> userTags, AuditHeader ah) {
        if ((userTags == null) || userTags.isEmpty())
            return;

        Company company = ah.getFortress().getCompany();

        for (String key : userTags.keySet()) {
            Tag tag = tagService.processTag(new TagInputBean(company, key));
            auditTagDao.save(tag, ah, userTags.get(key).toString());
        }
    }

    public Set<TagValue> findAuditTags(String auditKey) {
        AuditHeader header = auditService.getHeader(auditKey);
        return auditTagDao.getAuditTags(header);

    }

    public void updateTagValues(Set<TagValue> newValues) {

        auditTagDao.update(newValues);

    }
}
