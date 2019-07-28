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

package org.flockdata.engine.data.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.flockdata.data.Alias;
import org.flockdata.data.Company;
import org.flockdata.data.Tag;
import org.flockdata.engine.data.graph.AliasNode;
import org.flockdata.engine.data.graph.ConceptNode;
import org.flockdata.engine.data.graph.TagNode;
import org.flockdata.engine.tag.service.TagManager;
import org.flockdata.helper.FlockDataTagException;
import org.flockdata.helper.TagHelper;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.track.TagKey;
import org.flockdata.track.TagPayload;
import org.flockdata.track.bean.FdTagResultBean;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Service;

/**
 * Move to Neo4j server extension
 *
 * @author mholdsworth
 * @tag Tag, Neo4j
 * @since 20/06/2015
 */

@Service
public class TagWrangler {

  private final Neo4jTemplate template;

  private final AliasDaoNeo aliasDao;
  private final TagManager tagManager;

  private final ConceptTypeRepo conceptTypeRepo;

  private Logger logger = LoggerFactory.getLogger(TagWrangler.class);

  @Autowired
  public TagWrangler(Neo4jTemplate template, AliasDaoNeo aliasDao, TagManager tagManager, ConceptTypeRepo conceptTypeRepo) {
    this.template = template;
    this.aliasDao = aliasDao;
    this.tagManager = tagManager;
    this.conceptTypeRepo = conceptTypeRepo;
  }

  public Collection<FdTagResultBean> save(TagPayload payload) {
    Collection<FdTagResultBean> results = new ArrayList<>(payload.getTags().size());
    List<String> createdValues = new ArrayList<>();
    results.addAll(payload.getTags().stream().map(tagInputBean ->
        save(payload.getCompany(), tagInputBean, payload.getTenant(), createdValues, payload.isIgnoreRelationships())).collect(Collectors.toList()));
    return results;

  }

  // ToDo: Turn this in to ServerSide
  private FdTagResultBean save(Company company, TagInputBean tagInput, String tagSuffix, Collection<String> cachedValues, boolean suppressRelationships) {
    // Check exists
    boolean isNew = false;
    FdTagResultBean tagResultBean;
    TagNode startTag = findTag(tagSuffix, tagInput.getLabel(), tagInput.getKeyPrefix(), (tagInput.getCode() == null ? tagInput.getName() : tagInput.getCode()), false);
    boolean changed = false;
    if (startTag == null) {
      if (tagInput.isMustExist()) {

        tagInput.setServiceMessage("Tag [" + tagInput + "] should exist for [" + tagInput.getLabel() + "] but doesn't. Ignoring this request.");
        if (tagInput.getNotFoundCode() != null && !tagInput.getNotFoundCode().equals("")) {
          TagInputBean notFound = new TagInputBean(tagInput.getNotFoundCode())
              .setLabel(tagInput.getLabel());

          tagInput.setServiceMessage("Tag [" + tagInput + "] should exist as a [" + tagInput.getLabel() + "] but doesn't. Assigning to [" + tagInput.getNotFoundCode() + "]. An alias is been created for " + tagInput.getCode());
          logger.info(tagInput.setServiceMessage());
          ArrayList<AliasInputBean> aliases = new ArrayList<>();
          // Creating an alias so that we don't have to process this all again. The alias will be against the undefined tag.
          aliases.add(new AliasInputBean(tagInput.getCode()));
          notFound.setAliases(aliases);
          tagResultBean = save(company, notFound, tagSuffix, cachedValues, suppressRelationships);
          startTag = (TagNode) tagResultBean.getTag();
        } else {
          return new FdTagResultBean(tagInput);
        }
      } else {
        isNew = true;
        if (tagInput.getCode() == null) {
          throw new FlockDataTagException("The code property for a tag cannot be null {" + tagInput.toString());
        }
        startTag = createTag(tagInput, tagSuffix);
      }
    } else {
      // Existing Tag. We only update certain properties. Code is immutable (or near enough to)
      if (tagInput.isMerge()) {
        if (tagInput.hasTagProperties()) {
          for (String key : tagInput.getProperties().keySet()) {
            startTag.addProperty(key, tagInput.getProperty(key));
            changed = true;
          }
        }

        if (tagInput.getName() != null && !tagInput.getName().equals(startTag.getName())) {
          startTag.setName(tagInput.getName());
          changed = true;
        }

        if (changed) {
          startTag = tagManager.save(new TagKey(startTag));

        }
      }
    }
    FdTagResultBean sourceResult = new FdTagResultBean(tagInput, startTag, (isNew | changed));
    if (tagInput.hasAliases()) {
      handleAliases(tagInput, startTag);
    }

    if (tagInput.hasTargets()) {
      Map<String, Collection<TagInputBean>> targets = tagInput.getTargets();
      for (String rlxName : targets.keySet()) {
        Collection<TagInputBean> associatedTag = targets.get(rlxName);
        for (TagInputBean tagInputBean : associatedTag) {
          processAssociatedTags(company, tagSuffix, sourceResult, tagInputBean, rlxName, cachedValues, suppressRelationships);
        }

      }
    }

    return sourceResult;
  }

  private void handleAliases(TagInputBean tagInput, TagNode startTag) {
    String label = tagInput.getLabel();
    Collection<Alias> aliases = new ArrayList<>();
    for (AliasInputBean newAlias : tagInput.getAliases()) {

      AliasNode alias = aliasDao.findAlias(label, newAlias, startTag);
      alias.setTag(startTag);
      aliases.add(alias);
    }
    if (!aliases.isEmpty()) {
      template.fetch(startTag.getAliases());
    }
    for (Alias alias : aliases) {
      if (!TagHelper.hasAlias(startTag.getAliases(), label, alias.getKey())) {
        template.saveOnly(alias);
        startTag.addAlias(alias);
      }

    }
  }

  private TagNode createTag(TagInputBean tagInput, String suffix) {

    logger.trace("createTag {}", tagInput);
    // ToDo: Should a label be suffixed with company in multi-tenanted? - more time to think!!
    //       do we care that one company can see another companies tag value? Certainly not the
    //       track data.
    String label;
    if (tagInput.isDefault()) {
      label = Tag.DEFAULT_TAG + suffix;
    } else {
      label = tagInput.getLabel();
    }

    resolveKeyPrefix(suffix, tagInput);

    TagNode tag = new TagNode(tagInput, label);

    logger.trace("Saving {}", tag);
    tag = tagManager.save(tag);
    logger.debug("Saved {}", tag);
    return tag;

  }

  Map<String, Collection<FdTagResultBean>> findAllTags(Tag sourceTag, String relationship, String targetLabel) {
    String query = "match (t) -[" + (!relationship.equals("") ? "r:" + relationship : "r") + "]-(targetTag:" + targetLabel + ") where id(t)={id}  return r, targetTag";
    Map<String, Object> params = new HashMap<>();
    params.put("id", sourceTag.getId());
    Iterable<Map<String, Object>> result = template.query(query, params);
    Map<String, Collection<FdTagResultBean>> tagResults = new HashMap<>();
    for (Map<String, Object> mapResult : result) {
      TagNode n = template.projectTo(mapResult.get("targetTag"), TagNode.class);

      String rType = ((org.neo4j.graphdb.Relationship) mapResult.get("r")).getType().name();
      Collection<FdTagResultBean> tagResultBeans = tagResults.get(rType);
      if (tagResultBeans == null) {
        tagResultBeans = new ArrayList<>();
        tagResults.put(rType, tagResultBeans);
      }
      tagResultBeans.add(new FdTagResultBean(n));

    }
    return tagResults;

  }

  Collection<Tag> findDirectedTags(String tagSuffix, Tag startTag, Company company) {
    //Long coTags = getCompanyTagManager(companyId);
    //"MATCH track<-[tagType]-(tag:Tag"+engineAdmin.getTagSuffix(company)+") " +
    String query =
        " match (tag:Tag)-[]->(otherTag" + Tag.DEFAULT + tagSuffix + ") " +
            "   where id(tag)={tagId} return otherTag";
    Map<String, Object> params = new HashMap<>();
    params.put("tagId", startTag.getId());

    Iterable<Map<String, Object>> result = template.query(query, params);

    if (!((Result) result).iterator().hasNext()) {
      return new ArrayList<>();
    }

    Iterator<Map<String, Object>> rows = result.iterator();

    Collection<Tag> results = new ArrayList<>();

    while (rows.hasNext()) {
      Map<String, Object> row = rows.next();
      results.add(template.projectTo(row.get("otherTag"), TagNode.class));
    }
    //
    return results;
  }

  private void resolveKeyPrefix(String suffix, TagInputBean tagInput) {
    String prefix = resolveKeyPrefix(tagInput.getKeyPrefix(), suffix);
    if (prefix != null) {
      tagInput.setKeyPrefix(prefix);
    }
  }

  private String resolveKeyPrefix(String keyPrefix, String suffix) {
    if (keyPrefix != null && keyPrefix.contains(":")) {
      // Label:Value to set the prefix
      // DAT-479 indirect lookup
      String[] values = StringUtils.split(keyPrefix, ":");
      if (values.length == 2) {
        TagNode indirect = findTag(suffix, values[0], null, values[1], false);
        if (indirect == null) {
          // ToDo: Exception or literal?
          logger.debug("Indirect syntax was found but resolved to no tag");
          throw new AmqpRejectAndDontRequeueException("Unable to resolve the indirect tag" + keyPrefix);
        } else {
          return indirect.getCode();
        }

      }
    }
    return keyPrefix;
  }

  TagNode findTag(String suffix, String label, String keyPrefix, String tagCode, boolean inflate) {
    if (tagCode == null) {
      throw new IllegalArgumentException("Null can not be used to find a tag (" + label + ")");
    }

    String multiTennantedLabel = TagHelper.suffixLabel(label, suffix);
    String kp = resolveKeyPrefix(keyPrefix, suffix);

    TagNode tag = tagManager.tagByKey(new TagKey(multiTennantedLabel, kp, tagCode));
    if (tag != null && inflate) {
      template.fetch(tag.getAliases());
    }
    logger.trace("requested tag [{}:{}] foundTag [{}]", label, tagCode, (tag == null ? "NotFound" : tag));
    return tag;
  }

  /**
   * Create unique relationship between the tag and the node
   *
   * @param company               associate the tag with this company
   * @param tagSuffix
   * @param startTag              notional start node
   * @param associatedTag         tag to make or get
   * @param rlxName               relationship name
   * @param cachedValues          running list of values already created - performance op.
   * @param suppressRelationships @return the created tag
   */
  private void processAssociatedTags(Company company, String tagSuffix, FdTagResultBean startTag, TagInputBean associatedTag, String rlxName, Collection<String> cachedValues, boolean suppressRelationships) {

    FdTagResultBean endTag = save(company, associatedTag, tagSuffix, cachedValues, suppressRelationships);
    if (suppressRelationships) {
      return;
    }
    //Node endNode = template.getNode(tag.getId());

    Tag startId = (!associatedTag.isReverse() ? startTag.getTag() : endTag.getTag());
    Tag endId = (!associatedTag.isReverse() ? endTag.getTag() : startTag.getTag());

    if (endId == null || startId == null) {
      return;
    }

    String key = rlxName + ":" + startId.getId() + ":" + endId.getId();
    if (cachedValues.contains(key)) {
      return;
    }

    cachedValues.add(createRelationship(rlxName, startId, endId, key));
    startTag.addTargetResult(rlxName, endTag);
  }

  private String createRelationship(String rlxName, Tag startTag, Tag endTag, String key) {
//        if ((template.getRelationshipBetween(startTag, endTag, rlxName) == null))
//            template.createRelationshipBetween(template.getNode(startTag.getId()), template.getNode(endTag.getId()), rlxName, null);

    Node start = template.getNode(startTag.getId());
    //start.createRelationshipTo()
    Node end = template.getNode(endTag.getId());
    Relationship r = template.getOrCreateRelationship("rlxNames", "rlx", rlxName + ":" + startTag.getId() + ":" + endTag.getId(), start, end, rlxName, null);

    return key;
  }

  void createAlias(String suffix, Tag tag, String label, AliasInputBean aliasInput) {
    String theLabel = TagHelper.suffixLabel(label, suffix);
    template.fetch(tag);
    template.fetch(tag.getAliases());
    if (TagHelper.hasAlias(tag.getAliases(), theLabel, TagHelper.parseKey(aliasInput.getCode()))) {
      return;
    }

    Alias alias = new AliasNode(theLabel, aliasInput, TagHelper.parseKey(aliasInput.getCode()), tag);

    alias = template.save(alias);
    logger.debug(alias.toString());

  }

  public Collection<Tag> findTags(String label) {
    Collection<Tag> tagResults = new ArrayList<>();
    // ToDo: Match to company - something like this.....
    //match (t:Law)-[:_TagLabel]-(c:FDCompany) where id(c)=0  return t,c;
    //match (t:Law)-[*..2]-(c:FDCompany) where id(c)=0  return t,c;
    String query = "match (tag:`" + label + "`) return distinct (tag) as tag";
    // Look at PAGE
    Iterable<Map<String, Object>> results = template.query(query, null);
    for (Map<String, Object> row : results) {
      Object o = row.get("tag");
      TagNode t = template.projectTo(o, TagNode.class);
      tagResults.add(t);

    }
    return tagResults;
  }

  public Collection<TagResultBean> findTags() {
    Collection<TagResultBean> tagResults = new ArrayList<>();
    Result<ConceptNode> concepts = conceptTypeRepo.findAll();
    for (ConceptNode concept : concepts) {
      tagResults.add(new TagResultBean(concept));
    }
    return tagResults;

  }

}
