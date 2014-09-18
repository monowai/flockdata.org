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

package com.auditbucket.engine.service;

import com.auditbucket.engine.repo.neo4j.dao.TagDaoNeo4j;
import com.auditbucket.helper.Command;
import com.auditbucket.helper.FlockException;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.track.service.TagService;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
public class TagServiceNeo4j implements TagService {
    @Autowired
    private SecurityHelper securityHelper;

    @Autowired
    private TagDaoNeo4j tagDao;

    @Autowired
    EngineConfig engineConfig;

    private Logger logger = LoggerFactory.getLogger(TagServiceNeo4j.class);

    @Override
    public Tag createTag(Company company, TagInputBean tagInput) {
        return tagDao.save(company, tagInput);
    }

    /**
     *
     * @param company   who owns this collection
     * @param tagInputs tags to establish
     * @return tagInputs that failed processing
     */
    @Override
    @Async
    public Future<Collection<Tag>> makeTags(final Company company, final List<TagInputBean> tagInputs) throws ExecutionException, InterruptedException {
        Collection<Tag>failedInput= new ArrayList<>();
        class DLCommand implements Command {
            Collection<Tag> createdTags;
            private final List<TagInputBean> inputs;
            public DLCommand(List<TagInputBean> tagInputBeans) {
                this.inputs = tagInputBeans;
            }

            @Override
            public Command execute() {
                // Creates the relationships
                createdTags = tagDao.save(company, inputs);
                return this;
            }
        }

        List<List<TagInputBean>> splitList = Lists.partition(tagInputs, 5);
        for (List<TagInputBean> tagInputBeans : splitList) {
            DLCommand c = new DLCommand(tagInputBeans);
            try {
                try {
                    com.auditbucket.helper.DeadlockRetry.execute(c, "creating tags", 15);
                } catch (IOException e) {
                    logger.error("KV Error?", e);
                    throw new FlockException("KV Erro", e);
                }
            } catch (FlockException e) {
                logger.error(" Tag errors detected");
            }
            failedInput.addAll(c.createdTags);
        }
        return new AsyncResult<>(failedInput);
    }

    @Override
    public Tag findTag(Company company, String tagName) {
        return tagDao.findOne(company, tagName, Tag.DEFAULT);
    }


    @Override
    public Tag findTag(String tagName) {
        Company company = securityHelper.getCompany();
        if (company == null)
            return null;
        return findTag(company, tagName);
    }

    @Override
    public Collection<Tag> findDirectedTags(Tag startTag) {
        return tagDao.findDirectedTags(startTag, securityHelper.getCompany(), true); // outbound
    }

    @Override
    public Collection<Tag> findTags(Company company, String index) {
        return tagDao.findTags(company, index);
    }

    @Override
    public Tag findTag(Company company, String tagName, String index) {
        return tagDao.findOne(company, tagName, index);  //To change body of created methods use File | Settings | File Templates.
    }

    @Override
    public Collection<String> getExistingIndexes() {
        return tagDao.getExistingLabels();
    }

    @Override
    public void createTags(Company company, List<TagInputBean> tagInputs) throws FlockException, IOException, ExecutionException, InterruptedException {

        class EntityDeadlockRetry implements Command {
            Company company;
            List<TagInputBean>tagInputBeans;

            public EntityDeadlockRetry(Company company, List<TagInputBean> tagInputs) {
                this.company = company;
                this.tagInputBeans = tagInputs;
            }

            @Override
            public Command execute() throws FlockException, IOException {
                boolean suppressRelationships = true;
                tagDao.save(company, tagInputBeans, suppressRelationships);

                return this;
            }
        }

        EntityDeadlockRetry c = new EntityDeadlockRetry(company, tagInputs);
        com.auditbucket.helper.DeadlockRetry.execute(c, "create tags with no relationships", 10);
    }

    @Override
    public void purgeUnusedConcepts(Company company){
        tagDao.purgeUnusedConcepts(company);
    }

    @Override
    public void purgeType(Company company, String type) {
        tagDao.purge(company,type);
    }
}
