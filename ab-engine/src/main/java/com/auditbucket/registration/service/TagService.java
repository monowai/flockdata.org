/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
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

package com.auditbucket.registration.service;

import com.auditbucket.audit.model.DocumentType;
import com.auditbucket.dao.SchemaDao;
import com.auditbucket.dao.TagDao;
import com.auditbucket.engine.service.EngineConfig;
import com.auditbucket.helper.Command;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.Tag;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Handles management of a companies tags.
 * All tags belong to the company across their fortresses
 * <p/>
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 12:53 PM
 */

@Service
@Transactional
public class TagService {
    @Autowired
    private SecurityHelper securityHelper;

    @Autowired
    private TagDao tagDao;

    @Autowired
    private SchemaDao schemaDao;

    @Autowired
    EngineConfig engineConfig;


    private Logger logger = LoggerFactory.getLogger(TagService.class);

    public Tag processTag(TagInputBean inputBean) {
        Company company = securityHelper.getCompany();
        return processTag(company, inputBean);

    }

    public Tag processTag(Company company, TagInputBean tagInput) {
        //schemaDao.ensureIndex(company, tagInput);
        return tagDao.save(company, tagInput);
    }

    public Collection<TagInputBean> processTags(List<TagInputBean> tagInputs) {
        Company company = securityHelper.getCompany();
        return processTags(company, tagInputs);
    }

    public Collection<TagInputBean> processTags(final Company company, final List<TagInputBean> tagInputs) {
        //schemaDao.ensureUniqueIndexes(company, tagInputs);
        Future<Collection<TagInputBean>> future = makeTags(company, tagInputs);
        while (!future.isDone())
            Thread.yield();
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Processing tags", e);
        }
        return null;
    }

    /**
     *
     * @param company   who owns this collection
     * @param tagInputs tags to establish
     * @return tagInputs that failed processing
     */
    @Async
    public Future<Collection<TagInputBean>> makeTags(final Company company, final List<TagInputBean> tagInputs) {
        Collection<TagInputBean>failedInput= new ArrayList<>();
        class DLCommand implements Command {
            Collection<TagInputBean> failedInput;
            private final List<TagInputBean> inputs;
            public DLCommand(List<TagInputBean> tagInputBeans) {
                this.inputs = tagInputBeans;
            }

            @Override
            public Command execute() {
                // Creates the relationships
                failedInput = tagDao.save(company, inputs);
                return this;
            }
        }

        List<List<TagInputBean>> splitList = Lists.partition(tagInputs, 5);
        for (List<TagInputBean> tagInputBeans : splitList) {
            DLCommand c = new DLCommand(tagInputBeans);
            try {
                com.auditbucket.helper.DeadlockRetry.execute(c, "creating tags", 15);
            } catch (DatagioException e) {
                logger.error(" Tag errors detected");
            }
            failedInput.addAll(c.failedInput);
        }
        return new AsyncResult<>(failedInput);
    }

    public Tag findTag(Company company, String tagName) {
        return tagDao.findOne(company, tagName, Tag.DEFAULT);
    }


    public Tag findTag(String tagName) {
        Company company = securityHelper.getCompany();
        if (company == null)
            return null;
        return findTag(company, tagName);
    }

    /**
     *
     * @param fortress     system that has an interest
     * @param documentType name of the doc type
     * @return resolved document. Created if missing
     */
    public DocumentType resolveDocType(Fortress fortress, String documentType) {
        return resolveDocType(fortress, documentType, true);
    }

    /**
     * finds or creates a Document Type for the caller's company
     *
     * @param fortress        system that has an interest
     * @param documentType    name of the document
     * @param createIfMissing create document types that are missing
     * @return created DocumentType
     */
    public DocumentType resolveDocType(Fortress fortress, String documentType, Boolean createIfMissing) {
        if (documentType == null) {
            throw new IllegalArgumentException("DocumentTypeNode cannot be null");
        }

        return schemaDao.findDocumentType(fortress, documentType, createIfMissing);

    }

    public Collection<Tag> findDirectedTags(Tag startTag) {
        return tagDao.findDirectedTags(startTag, securityHelper.getCompany(), true); // outbound
    }

    public Map<String, Tag> findTags(String index) {
        Company company = securityHelper.getCompany();
        return findTags(company, index);
    }

    public Map<String, Tag> findTags(Company company, String index) {
        if (!index.startsWith(":"))
            index = ":" + index;
        return tagDao.findTags(company, index);
    }

    public Tag findTag(Company company, String tagName, String index) {
        if (!index.startsWith(":"))
            index = ":" + index;
        return tagDao.findOne(company, tagName, index);  //To change body of created methods use File | Settings | File Templates.
    }

    public Collection<String> getExistingIndexes() {
        return tagDao.getExistingIndexes();
    }

    public void createTagsNoRelationships(Company company, List<TagInputBean> tagInputs) {
        boolean suppressRelationships = true;
        tagDao.save(company, tagInputs, suppressRelationships);
    }
}
