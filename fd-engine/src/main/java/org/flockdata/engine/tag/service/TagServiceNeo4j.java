/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.flockdata.authentication.SecurityHelper;
import org.flockdata.data.Company;
import org.flockdata.data.Tag;
import org.flockdata.engine.configure.EngineConfig;
import org.flockdata.engine.data.dao.ConceptDaoNeo;
import org.flockdata.engine.data.dao.TagDaoNeo4j;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.TagNode;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.track.TagPayload;
import org.flockdata.track.bean.FdTagResultBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles management of a companies tags.
 * All tags belong to the company across their fortresses
 *
 * @author mholdsworth
 * @since 29/06/2013
 */

@Service
@Transactional
public class TagServiceNeo4j implements TagService {
  private final EngineConfig engineConfig;
  private final SecurityHelper securityHelper;
  private final TagDaoNeo4j tagDaoNeo4j;
  private final ConceptDaoNeo conceptDao;
  private final Neo4jTemplate template;
  private Logger logger = LoggerFactory.getLogger(TagServiceNeo4j.class);

  @Autowired
  public TagServiceNeo4j(EngineConfig engineConfig, ConceptDaoNeo conceptDao, TagDaoNeo4j tagDaoNeo4j, SecurityHelper securityHelper, Neo4jTemplate template) {
    this.engineConfig = engineConfig;
    this.conceptDao = conceptDao;
    this.tagDaoNeo4j = tagDaoNeo4j;
    this.securityHelper = securityHelper;
    this.template = template;
  }

  @Override
  public FdTagResultBean createTag(Company company, TagInputBean tagInput) throws FlockException {
    Collection<TagInputBean> tags = new ArrayList<>();
    tags.add(tagInput);

    Collection<FdTagResultBean> results = createTags(company, tags);
    if (results.isEmpty()) {
      return null;
    }

    FdTagResultBean tagResult = results.iterator().next();

    if (tagResult.getTag() == null) {
      throw new FlockException(tagResult.getMessage());
    }
    return tagResult;
  }

  @Override
  public Collection<FdTagResultBean> createTags(Company company, Collection<TagInputBean> tagInputs) throws FlockException {
    CompanyNode fdCompany = (CompanyNode) company;
    String tenant = engineConfig.getTagSuffix(fdCompany);

    TagPayload payload = new TagPayload(fdCompany)
        .setTags(tagInputs)
        .setTenant(tenant)
        .setIgnoreRelationships(false);

    Collection<FdTagResultBean> results = tagDaoNeo4j.save(payload);

    for (FdTagResultBean result : results) {
      conceptDao.registerTag(fdCompany, result);
    }
    return results;
  }

  @Override
  public Tag findTag(Company company, String keyPrefix, String tagCode) {
    return findTag(company, Tag.DEFAULT, keyPrefix, tagCode);
  }

  @Override
  public Collection<Tag> findDirectedTags(Tag startTag) {
    Company company = securityHelper.getCompany();
    String suffix = engineConfig.getTagSuffix(company);
    return tagDaoNeo4j.findDirectedTags(suffix, startTag, company);
  }

  @Override
  public Collection<FdTagResultBean> findTagResults(Company company, String label) {
    Collection<Tag> tags = tagDaoNeo4j.findTags(label);
    Collection<FdTagResultBean> countries = new ArrayList<>(tags.size());
    for (Tag tag : tags) {
      template.fetch(tag.getAliases());
      countries.add(new FdTagResultBean(tag));
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
      return findTag((CompanyNode) company, label, keyPrefix, tagCode, false);
    } catch (NotFoundException e) {
      logger.debug("findTag notFound {}, {}", tagCode, label);
    }
    return null;
  }

  @Override
  public Tag findTag(Company company, String label, String keyPrefix, String tagCode, boolean inflate) throws NotFoundException {
    String suffix = engineConfig.getTagSuffix(company);

    Tag tag = tagDaoNeo4j.findTagNode(suffix, label, keyPrefix, tagCode, inflate);

    if (tag == null) {
      throw new NotFoundException("Tag [" + label + "]/[" + tagCode + "] not found");

    }
    return tag;
  }

  @Override
  public void createAlias(Company company, Tag tag, String forLabel, String aliasKeyValue) {
    AliasInputBean aliasInputBean = new AliasInputBean(aliasKeyValue);
    createAlias(company, tag, forLabel, aliasInputBean);
  }

  public void createAlias(Company company, Tag tag, String forLabel, AliasInputBean aliasInput) {
    String suffix = engineConfig.getTagSuffix(company);
    tagDaoNeo4j.createAlias(suffix, tag, forLabel, aliasInput);
  }

  /**
   * Returns tags connected to the source tag
   *
   * @param company     callers company
   * @param sourceLabel label to start search with
   * @param sourceCode  code of a specific tag
   * @param targetLabel find all tags of this type - no relationship filter
   * @return StartTag and collection of connected tags
   * @throws NotFoundException source tag not found by sourceLabel + sourceCode
   */
  @Override
  public Map<String, Collection<FdTagResultBean>> findTags(Company company, String sourceLabel, String sourceCode, String relationship, String targetLabel) throws NotFoundException {
    Tag source = findTag(company, sourceLabel, null, sourceCode);
    if (source == null) {
      throw new NotFoundException("Unable to find the requested tag " + sourceCode);
    }
    if (relationship == null || relationship.equals("*")) {
      relationship = "";
    }
    return tagDaoNeo4j.findAllTags(source, relationship, targetLabel);
  }

  @Override
  public Collection<TagNode> findTag(Company company, String code) {
    Collection<TagNode> results = new ArrayList<>();

    TagNode t = (TagNode) findTag(company, null, code);
    if (t != null) {
      results.add(t);
    }
    return results;
  }

  @Override
  public Collection<AliasInputBean> findTagAliases(Company company, String label, String keyPrefix, String tagCode) throws NotFoundException {
    Tag source = findTag(company, label, keyPrefix, tagCode);
    if (source == null) {
      throw new NotFoundException("Unable to find the requested tag " + tagCode);
    }

    return tagDaoNeo4j.findTagAliases(source);
  }
}
