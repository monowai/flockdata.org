/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

import org.flockdata.authentication.registration.bean.AliasInputBean;
import org.flockdata.authentication.registration.bean.TagInputBean;
import org.flockdata.authentication.registration.bean.TagResultBean;
import org.flockdata.configure.SecurityHelper;
import org.flockdata.engine.PlatformConfig;
import org.flockdata.engine.tag.dao.TagDaoNeo4j;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.meta.dao.ConceptDaoNeo;
import org.flockdata.model.Company;
import org.flockdata.model.Tag;
import org.flockdata.track.TagPayload;
import org.flockdata.track.service.TagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Handles management of a companies tags.
 * All tags belong to the company across their fortresses
 * <p>
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
    private ConceptDaoNeo conceptDao;

    @Autowired
    PlatformConfig engineConfig;

    private Logger logger = LoggerFactory.getLogger(TagServiceNeo4j.class);

    @Override
    public Tag createTag(Company company, TagInputBean tagInput) throws FlockException {
        Collection<TagInputBean>tags = new ArrayList<>();
        tags.add(tagInput);

        Collection<TagResultBean>results = createTags(company, tags);
        if ( results.isEmpty())
            return null;

        TagResultBean tagResult =   results.iterator().next();

        if ( tagResult.getTag() == null  )
            throw new AmqpRejectAndDontRequeueException(tagResult.getMessage());
        return tagResult.getTag();
    }

    @Override
    public Collection<TagResultBean> createTags(Company company, Collection<TagInputBean> tagInputs) throws FlockException{
        String tenant = engineAdmin.getTagSuffix(company);

        TagPayload payload = new TagPayload(company)
                .setTags(tagInputs)
                .setTenant(tenant)
                .setIgnoreRelationships(false);

        Collection<TagResultBean> results = tagDao.save(payload);

        for (TagResultBean result : results) {
            if ( result.isNew() && !result.getTag().isDefault() )
                conceptDao.registerTag(company, result.getTag().getLabel());
        }
        return results;
    }

    @Override
    public Tag findTag(Company company, String keyPrefix, String tagCode) {
        return findTag(company, Tag.DEFAULT,keyPrefix , tagCode);
    }

    @Override
    public Collection<Tag> findDirectedTags(Tag startTag) {
        Company company = securityHelper.getCompany();
        String suffix = engineAdmin.getTagSuffix(company);
        return tagDao.findDirectedTags(suffix, startTag, company);
    }

    @Override
    public Collection<Tag> findTags(Company company, String label) {
        return tagDao.findTags(label);
    }

    @Override
    public Tag findTag(Company company, String label, String keyPrefix, String tagCode) {
        try {
            return findTag(company, label, keyPrefix, tagCode, false);
        } catch (NotFoundException e){
            logger.debug("findTag notFound {}, {}", tagCode, label);
        }
        return null;
    }

    @Override
    public Tag findTag(Company company, String label, String keyPrefix, String tagCode, boolean inflate) throws NotFoundException {
        String suffix = engineAdmin.getTagSuffix(company);

        Tag tag = tagDao.findTagNode(suffix, label, keyPrefix, tagCode, inflate);

        if (tag == null) {
            throw new NotFoundException("Tag "+label +"/"+tagCode +" not found");

        }
        return tag;
    }

    @Autowired
    PlatformConfig engineAdmin;

    @Override
    public void createAlias(Company company, Tag tag, String forLabel, String aliasKeyValue) {
        AliasInputBean aliasInputBean = new AliasInputBean(aliasKeyValue);
        createAlias(company, tag, forLabel, aliasInputBean);
    }

    public void createAlias(Company company, Tag tag, String forLabel, AliasInputBean aliasInput) {
        String suffix = engineAdmin.getTagSuffix(company);
        tagDao.createAlias(suffix, tag, forLabel, aliasInput);
    }

    /**
     * Returns all tags with the
     * @param company       callers company
     * @param sourceLabel   label to start search with
     * @param sourceCode    code of a specific tag
     * @param targetLabel   find all tags of this type - no relationship filter
     * @return
     * @throws NotFoundException
     */
    @Override
    public Map<String, Collection<TagResultBean>> findTags(Company company, String sourceLabel, String sourceCode, String relationship, String targetLabel) throws NotFoundException {
        Tag source = findTag(company, sourceLabel,null , sourceCode);
        if (source == null)
            throw new NotFoundException("Unable to find the requested tag " + sourceCode);
        if ( relationship == null || relationship.equals("*"))
            relationship = "";
        return tagDao.findAllTags(source, relationship, targetLabel);
    }

    @Override
    public Collection<Tag> findTag(Company company, String code) {
        Collection<Tag>results = new ArrayList<>();

        Tag t = findTag(company, null, code);
        if ( t !=null )
            results.add(t);
        return results;
    }

    @Override
    public Collection<AliasInputBean> findTagAliases(Company company, String label, String keyPrefix, String tagCode) throws NotFoundException {
        Tag source = findTag(company, label, keyPrefix, tagCode);
        if (source == null)
            throw new NotFoundException("Unable to find the requested tag " + tagCode);

        return tagDao.findTagAliases(source);
    }
}
