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
import com.auditbucket.audit.model.AuditTag;
import com.auditbucket.bean.AuditTagInputBean;
import com.auditbucket.dao.AuditTagDao;
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
    TagService tagService;

    @Autowired
    SecurityHelper securityHelper;

    @Autowired
    AuditTagDao auditTagDao;

    public AuditTag processTag(AuditHeader header, AuditTagInputBean tagInput) {
        //Company company = securityHelper.getCompany();
        String type = tagInput.getType();
        Set<AuditTag> existing = findTagValues(tagInput.getTagName(), type);
        if (existing != null && existing.size() == 1)
            // We already have this tagged so get out of here
            return existing.iterator().next();

        Tag tag = tagService.processTag(new TagInputBean(tagInput.getTagName()));
        return auditTagDao.save(header, tag, type);
    }

    public Set<AuditTag> findTagValues(String name, String type) {
        Tag tag = tagService.findTag(name);
        if (tag == null)
            return null;
        return auditTagDao.find(tag, type);
    }

    public Set<AuditHeader> findTagAudits(String tagName) {
        Tag tag = tagService.findTag(tagName);
        if (tag == null)
            return null;
        return auditTagDao.findTagAudits(tag);
    }

    /**
     * Will associate the supplied userTags with the AuditHeaderNode
     * <p/>
     * The Key in the map will be treated as the tag name if the value == null
     * Otherwise the key is treated as the tagType and the value is treated as the name.
     * <p/>
     * This approach allows you to use a simple tag for a document such as
     * ClientID123 without having to describe the type (it will be created as a general type)
     * <p/>
     * likewise if providing Type/Name then you can associate the same name tag with multiple relationships types
     * <p/>
     * clientKey/ClientID123
     * prospectKey/ClientID123
     * <p/>
     * If this scenario, ClientID123 is created as a single node with two relationships - clientKey and prospectKey
     *
     * @param userTags Key/Value pair of tags. TagNode will be created if missing
     * @param ah       Header to associate userTags with
     */
    public void createTagValues(Map<String, String> userTags, AuditHeader ah) {
        if ((userTags == null) || userTags.isEmpty())
            return;

        Company company = ah.getFortress().getCompany();

        for (String tagType : userTags.keySet()) {
            Tag tag;
            String tagName = userTags.get(tagType);
            if (tagName == null)
                tag = tagService.processTag(new TagInputBean(company, tagType));
            else
                tag = tagService.processTag(new TagInputBean(company, tagName));

            auditTagDao.save(ah, tag, tagType);
        }
    }

    public Set<AuditTag> findAuditTags(AuditHeader auditHeader) {
        return auditTagDao.getAuditTags(auditHeader.getId());
    }

    public Set<AuditTag> findAuditTags(Long id) {
        return auditTagDao.getAuditTags(id);
    }
}
