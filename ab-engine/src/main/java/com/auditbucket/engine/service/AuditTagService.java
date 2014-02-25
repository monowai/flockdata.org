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

import com.auditbucket.audit.bean.AuditTagInputBean;
import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.AuditTag;
import com.auditbucket.dao.AuditTagDao;
import com.auditbucket.helper.AuditException;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.service.TagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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

    private Logger logger = LoggerFactory.getLogger(AuditTagService.class);

    public void processTag(AuditHeader header, AuditTagInputBean tagInput) {
        String relationshipName = tagInput.getType();
        boolean existing = relationshipExists(header, tagInput.getTagName(), relationshipName);
        if (existing)
            // We already have this tagged so get out of here
            return;
        Tag tag = tagService.findTag(tagInput.getTagName());
        auditTagDao.save(header, tag, relationshipName);
    }

    public Boolean relationshipExists(AuditHeader auditHeader, String name, String relationshipType) {
        Tag tag = tagService.findTag(name);
        if (tag == null)
            return false;
        return auditTagDao.relationshipExists(auditHeader, tag, relationshipType);
    }

    /**
     * Directed tag structure hierarchy
     *
     * @param userTags input beans
     * @param company  valid company
     */
    public void createTagStructure(Collection<TagInputBean> userTags, Company company) {
        // Create a tag structure if present
        for (TagInputBean inputBean : userTags) {
            Tag t = tagService.processTag(inputBean, company);
            if (t == null) {
                logger.error("Error creating Tag " + inputBean);
            }
        }
    }

    /**
     * Associates the supplied userTags with the AuditHeaderNode
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
    public Set<AuditTag> associateTags(AuditHeader ah, Collection<TagInputBean> userTags) {
        Set<AuditTag> rlxs = new TreeSet<>();
        if ((userTags == null) || userTags.isEmpty())
            return rlxs;

        Company company = ah.getFortress().getCompany();
        tagService.processTags(userTags, company);
        for (TagInputBean tagInput : userTags) {

            Tag tag = tagService.findTag(tagInput.getName(), company);

            // Handle both simple relationships type name or a map/collection of relationships
            if (tagInput.getAuditLinks() != null) {
                rlxs = processRelationships(ah, tag, tagInput.getAuditLinks());
            }
            if (tagInput.getAuditLink() != null) // Simple relationship to the audit header
                // Makes it easier for the API to call
                rlxs.add(auditTagDao.save(ah, tag, tagInput.getAuditLink()));

        }
        return rlxs;
    }

    private Set<AuditTag> processRelationships(AuditHeader ah, Tag tag, Map<String, Object> auditRelationships) {
        Set<AuditTag> rlxs = new TreeSet<>();
        for (String key : auditRelationships.keySet()) {
            Object properties = auditRelationships.get(key);
            Map<String, Object> propMap;
            if (properties != null && properties instanceof Map) {
                propMap = (Map<String, Object>) properties;
                AuditTag auditTagRelationship = auditTagDao.save(ah, tag, key, propMap);
                if (auditTagRelationship != null)
                    rlxs.add(auditTagRelationship);
            } else {
                AuditTag auditTagRelationship = auditTagDao.save(ah, tag, key);
                if (auditTagRelationship != null)
                    rlxs.add(auditTagRelationship);
            }
        }
        return rlxs;
    }

    public Set<AuditTag> findAuditTags(AuditHeader auditHeader) {
        Company company = securityHelper.getCompany();
        return findAuditTags(company, auditHeader);
    }

    public Set<AuditTag> findAuditTags(Company company, AuditHeader auditHeader) {
        return auditTagDao.getAuditTags(auditHeader, company);
    }

    public void deleteAuditTags(AuditHeader auditHeader, Collection<AuditTag> auditTags) throws AuditException {
        auditTagDao.deleteAuditTags(auditHeader, auditTags);
    }

    public void deleteAuditTag(AuditHeader auditHeader, AuditTag value) throws AuditException {
        Collection<AuditTag> remove = new ArrayList<>(1);
        remove.add(value);
        deleteAuditTags(auditHeader, remove);

    }

    public void changeType(AuditHeader auditHeader, AuditTag existingTag, String newType) throws AuditException {
        if (auditHeader == null || existingTag == null || newType == null)
            throw new AuditException(("Illegal parameter"));
        auditTagDao.changeType(auditHeader, existingTag, newType);
    }


    public Set<AuditHeader> findTagAudits(String tagName) throws AuditException {
        Tag tag = tagService.findTag(tagName);
        if (tag == null)
            throw new AuditException("Unable to find the tag [" + tagName + "]");
        return auditTagDao.findTagAudits(tag);

    }
}
