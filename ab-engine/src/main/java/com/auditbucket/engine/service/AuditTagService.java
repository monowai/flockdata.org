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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 27/06/13
 * Time: 5:07 PM
 */
@Service
@Transactional
public class AuditTagService {

    @Autowired
    TagService tagService;

    @Autowired
    SecurityHelper securityHelper;

    @Autowired
    AuditTagDao auditTagDao;

    public void processTag(AuditHeader header, AuditTagInputBean tagInput) {
        //Company company = securityHelper.getCompany();
        String type = tagInput.getType();
        Set<AuditTag> existing = findTagValues(tagInput.getTagName(), type);
        if (existing != null && existing.size() == 1)
            // We already have this tagged so get out of here
            return;

        Tag tag = tagService.processTag(new TagInputBean(tagInput.getTagName()));
        auditTagDao.save(header, tag, type);
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
        return auditTagDao.findTagAudits(tag.getId());
    }


    /**
     * Will associate the supplied userTags with the AuditHeaderNode
     * <p/>
     * in JSON terms....
     * "ClientID123" :{"clientKey","prospectKey"}
     * <p/>
     * <p/>
     * The value can be null which will create a simple tag for the Header such as
     * ClientID123
     * <p/>
     * They type can be Null, String or a Collection<String> that describes the relationship
     * types to create.
     * <p/>
     * If this scenario, ClientID123 is created as a single node with two relationships that
     * describe the association - clientKey and prospectKey
     *
     * @param ah       Header to associate userTags with
     * @param userTags Key/Value pair of tags. TagNode will be created if missing. Value can be a Collection
     */
    @Caching(evict = {@CacheEvict(value = "auditHeaderId", key = "#p0.id"),
            @CacheEvict(value = "auditKey", key = "#p0.auditKey")})
    public void createTagValues(AuditHeader ah, Map<String, Object> userTags) {
        if ((userTags == null) || userTags.isEmpty())
            return;

        Company company = ah.getFortress().getCompany();

        for (String tagName : userTags.keySet()) {
            Tag tag = tagService.processTag(new TagInputBean(company, tagName));
            Object tagRlx = userTags.get(tagName);
            String rlxName;
            // Handle both a simple relationship type name or a collection of relationships
            if (tagRlx == null)
                auditTagDao.save(ah, tag, null);

            else {

                if (tagRlx instanceof Collection) {
                    for (Object o : ((Collection) tagRlx)) {
                        auditTagDao.save(ah, tag, o.toString());
                    }
                } else {
                    rlxName = tagRlx.toString();
                    auditTagDao.save(ah, tag, rlxName);
                }

            }

        }
    }

    public Set<AuditTag> findAuditTags(AuditHeader auditHeader) {
        return auditTagDao.getAuditTags(auditHeader.getId());
    }

    public Set<AuditTag> findAuditTags(Long id) {
        return auditTagDao.getAuditTags(id);
    }
}
