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

package org.flockdata.engine.tag.dao;

import org.flockdata.engine.concept.service.ConceptDaoNeo4j;
import org.flockdata.engine.schema.IndexRetryService;
import org.flockdata.engine.tag.model.AliasNode;
import org.flockdata.engine.tag.model.TagNode;
import org.flockdata.helper.NotFoundException;
import org.flockdata.registration.bean.AliasInputBean;
import org.flockdata.registration.bean.TagResultBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Tag;
import org.flockdata.track.TagPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 8:33 PM
 */
@Repository
public class TagDaoNeo4j {

    @Autowired
    ConceptDaoNeo4j conceptDao;

    @Autowired
    Neo4jTemplate template;


    @Autowired
    TagWriter tagWriter;

    @Autowired
    IndexRetryService indexRetryService;

    private Logger logger = LoggerFactory.getLogger(TagDaoNeo4j.class);

    public Collection<TagResultBean>save(TagPayload payload){
        return tagWriter.save( payload);
    }

    public void createAlias(String suffix, Tag tag, String label, AliasInputBean aliasInput) {
        tagWriter.createAlias(suffix, tag, label, aliasInput);
    }


    public Collection<Tag> findDirectedTags(String tagSuffix, Tag startTag, Company company) {
        //Long coTags = getCompanyTagManager(companyId);
        //"MATCH track<-[tagType]-(tag:Tag"+engineAdmin.getTagSuffix(company)+") " +
        String query =
                " match (tag:Tag)-[]->(otherTag" + Tag.DEFAULT + tagSuffix + ") " +
                        "   where id(tag)={tagId} return otherTag";
        Map<String, Object> params = new HashMap<>();
        params.put("tagId", startTag.getId());

        Iterable<Map<String, Object>> result = template.query(query, params);

        if (!((Result) result).iterator().hasNext())
            return new ArrayList<>();

        Iterator<Map<String, Object>> rows = result.iterator();

        Collection<Tag> results = new ArrayList<>();

        while (rows.hasNext()) {
            Map<String, Object> row = rows.next();
            results.add(template.projectTo(row.get("otherTag"), TagNode.class));
        }
        //
        return results;
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
            Tag t = template.projectTo(o, TagNode.class);
            tagResults.add(t);

        }
        return tagResults;
    }


    public Collection<AliasInputBean> findTagAliases(Tag sourceTag) throws NotFoundException {

        String query = "match (t) -[:HAS_ALIAS]->(alias) where id(t)={id}  return alias";
        Map<String, Object> params = new HashMap<>();
        params.put("id", sourceTag.getId());
        Iterable<Map<String, Object>> result = template.query(query, params);
        Collection<AliasInputBean> aliasResults = new ArrayList<>();
        for (Map<String, Object> mapResult : result) {
            AliasNode n = template.projectTo(mapResult.get("alias"), AliasNode.class);
            aliasResults.add(new AliasInputBean(n.getName()));

        }
        return aliasResults;
    }

    public Map<String, Collection<TagResultBean>> findAllTags(Tag sourceTag, String relationship, String targetLabel) {
        String query = "match (t) -["+ (!relationship.equals("")? "r:"+relationship :"r")+"]-(targetTag:"+targetLabel+") where id(t)={id}  return r, targetTag";
        Map<String, Object> params = new HashMap<>();
        params.put("id", sourceTag.getId());
        Iterable<Map<String, Object>> result = template.query(query, params);
        Map<String,Collection<TagResultBean>> tagResults = new HashMap<>();
        for (Map<String, Object> mapResult : result) {
            TagNode n = template.projectTo(mapResult.get("targetTag"), TagNode.class);

            String rType= ((org.neo4j.graphdb.Relationship)mapResult.get("r")).getType().name();
            Collection<TagResultBean>tagResultBeans = tagResults.get(rType);
            if ( tagResultBeans == null ){
                tagResultBeans = new ArrayList<>();
                tagResults.put(rType, tagResultBeans);
            }
            tagResultBeans.add(new TagResultBean(n));

        }
        return tagResults;

    }
    public Tag findTagNode(String suffix, String label, String tagCode, boolean inflate){
        return tagWriter.findTagNode(suffix, label, tagCode, inflate);
    }
}
