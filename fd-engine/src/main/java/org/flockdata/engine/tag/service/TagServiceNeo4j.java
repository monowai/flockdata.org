/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.tag.service;

import org.flockdata.engine.FdEngineConfig;
import org.flockdata.engine.tag.model.TagDaoNeo4j;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.SecurityHelper;
import org.flockdata.registration.bean.AliasInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Tag;
import org.flockdata.track.service.TagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
    FdEngineConfig engineConfig;

    private Logger logger = LoggerFactory.getLogger(TagServiceNeo4j.class);

    @Override
    public Tag createTag(Company company, TagInputBean tagInput) {
        return tagDao.save(company, tagInput);
    }

    @Override
    public Tag findTag(Company company, String tagCode) {
        return tagDao.findTag(company, tagCode, Tag.DEFAULT);
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
    public Collection<Tag> findTags(Company company, String label) {
        return tagDao.findTags(company, label);
    }

    @Override
    public Tag findTag(Company company, String label, String tagCode) {
        return tagDao.findTag(company, tagCode, label);
    }

    @Override
    public Collection<String> getExistingIndexes() {
        return tagDao.getExistingLabels();
    }

    @Override
    public Collection<Tag> createTags(Company company, List<TagInputBean> tagInputs) throws FlockException, IOException, ExecutionException, InterruptedException {
        return tagDao.save(company, tagInputs);
    }

    @Override
    public void purgeUnusedConcepts(Company company){
        tagDao.purgeUnusedConcepts(company);
    }

    @Override
    public void purgeLabel(Company company, String label) {
        tagDao.purge(company, label);
    }

    @Override
    public void createAlias(Company company, Tag tag, String forLabel, String aliasKeyValue) {
        AliasInputBean aliasInputBean = new AliasInputBean(aliasKeyValue);
        createAlias(company, tag, forLabel, aliasInputBean);
    }

    public void createAlias(Company company, Tag tag, String forLabel, AliasInputBean aliasInput) {

        tagDao.createAlias(company, tag, forLabel, aliasInput);
    }
}
