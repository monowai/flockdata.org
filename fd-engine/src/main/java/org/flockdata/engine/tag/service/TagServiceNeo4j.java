/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.tag.service;

import org.flockdata.engine.PlatformConfig;
import org.flockdata.engine.configure.SecurityHelper;
import org.flockdata.engine.dao.ConceptDaoNeo;
import org.flockdata.engine.dao.TagDaoNeo4j;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.Company;
import org.flockdata.model.Tag;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.track.TagPayload;
import org.flockdata.track.service.TagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
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
    private final SecurityHelper securityHelper;

    private final TagDaoNeo4j tagDaoNeo4j;

    private final ConceptDaoNeo conceptDao;

    private final Neo4jTemplate template;

    private Logger logger = LoggerFactory.getLogger(TagServiceNeo4j.class);

    @Autowired
    public TagServiceNeo4j(PlatformConfig engineAdmin, ConceptDaoNeo conceptDao, TagDaoNeo4j tagDaoNeo4j, SecurityHelper securityHelper, Neo4jTemplate template) {
        this.engineAdmin = engineAdmin;
        this.conceptDao = conceptDao;
        this.tagDaoNeo4j = tagDaoNeo4j;
        this.securityHelper = securityHelper;
        this.template = template;
    }

    @Override
    public TagResultBean createTag(Company company, TagInputBean tagInput) throws FlockException {
        Collection<TagInputBean>tags = new ArrayList<>();
        tags.add(tagInput);

        Collection<TagResultBean>results = createTags(company, tags);
        if ( results.isEmpty())
            return null;

        TagResultBean tagResult =   results.iterator().next();

        if ( tagResult.getTag() == null  )
            throw new AmqpRejectAndDontRequeueException(tagResult.getMessage());
        return tagResult;
    }

    @Override
    public Collection<TagResultBean> createTags(Company company, Collection<TagInputBean> tagInputs) throws FlockException{
        String tenant = engineAdmin.getTagSuffix(company);

        TagPayload payload = new TagPayload(company)
                .setTags(tagInputs)
                .setTenant(tenant)
                .setIgnoreRelationships(false);

        Collection<TagResultBean> results = tagDaoNeo4j.save(payload);

        for (TagResultBean result : results) {
            conceptDao.registerTag(company, result);
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
        return tagDaoNeo4j.findDirectedTags(suffix, startTag, company);
    }

    @Override
    public Collection<TagResultBean> findTagResults(Company company, String label) {
        Collection<Tag> tags = tagDaoNeo4j.findTags(label);
        Collection<TagResultBean>countries = new ArrayList<>(tags.size());
        for (Tag tag : tags) {
            template.fetch(tag.getAliases());
            countries.add(new TagResultBean(tag));
        }
//        countries.addAll(tags.stream().map(TagResultBean::new).collect(Collectors.toList()));
        return countries;
    }

    @Override
    public Collection<Tag> findTags(Company company, String label) {
        return tagDaoNeo4j.findTags(label);
    }

    @Override
    public Collection<TagResultBean> findTags(Company company) {
        return tagDaoNeo4j.findTags();
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

        Tag tag = tagDaoNeo4j.findTagNode(suffix, label, keyPrefix, tagCode, inflate);

        if (tag == null) {
            throw new NotFoundException("Tag ["+label +"]/["+tagCode +"] not found");

        }
        return tag;
    }

    final
    PlatformConfig engineAdmin;

    @Override
    public void createAlias(Company company, Tag tag, String forLabel, String aliasKeyValue) {
        AliasInputBean aliasInputBean = new AliasInputBean(aliasKeyValue);
        createAlias(company, tag, forLabel, aliasInputBean);
    }

    public void createAlias(Company company, Tag tag, String forLabel, AliasInputBean aliasInput) {
        String suffix = engineAdmin.getTagSuffix(company);
        tagDaoNeo4j.createAlias(suffix, tag, forLabel, aliasInput);
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
        return tagDaoNeo4j.findAllTags(source, relationship, targetLabel);
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

        return tagDaoNeo4j.findTagAliases(source);
    }
}
