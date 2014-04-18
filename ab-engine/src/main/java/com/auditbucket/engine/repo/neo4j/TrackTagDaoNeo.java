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

package com.auditbucket.engine.repo.neo4j;

import com.auditbucket.dao.TagDao;
import com.auditbucket.dao.TrackTagDao;
import com.auditbucket.engine.repo.neo4j.model.MetaHeaderNode;
import com.auditbucket.engine.repo.neo4j.model.TrackTagRelationship;
import com.auditbucket.engine.service.EngineConfig;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.repo.neo4j.model.TagNode;
import com.auditbucket.registration.service.TagService;
import com.auditbucket.track.model.GeoData;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.TrackTag;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * Data Access Object that manipulates tag nodes against track headers
 * <p/>
 * User: Mike Holdsworth
 * Date: 28/06/13
 * Time: 11:07 PM
 */
@Repository("auditTagDAO")
public class TrackTagDaoNeo implements TrackTagDao {
    @Autowired
    Neo4jTemplate template;

    @Autowired
    TagService tagService;

    private Logger logger = LoggerFactory.getLogger(TrackTagDaoNeo.class);

    @Override
    public TrackTag save(MetaHeader metaHeader, Tag tag, String relationshipName) {
        return save(metaHeader, tag, relationshipName, false, null);
    }
    @Override
    public TrackTag save(MetaHeader ah, Tag tag, String metaLink, boolean reverse) {
        return save(ah, tag, metaLink, reverse, null );
    }


    /**
     * creates the relationship between the metaHeader and the tag of the name type.
     * If auditId == null, then an TrackTag for the caller to deal with otherwise the relationship
     * is persisted and null is returned.
     *
     *
     * @param metaHeader      constructed metaHeader
     * @param tag              tag
     * @param relationshipName name
     * @param isReversed
     *@param propMap          properties to associate with an track tag (weight)  @return Null or TrackTag
     */
    public TrackTag save(MetaHeader metaHeader, Tag tag, String relationshipName, Boolean isReversed, Map<String, Object> propMap) {
        // ToDo: this will only set properties for the "current" tag to Header. it will not version it.
        if (relationshipName == null) {
            relationshipName = "GENERAL_TAG";
        }
        if (tag == null)
            throw new IllegalArgumentException("Tag must not be NULL. Relationship[" + relationshipName + "]");

        TrackTagRelationship rel = new TrackTagRelationship(metaHeader, tag, relationshipName, propMap);

        if (metaHeader.getId() == null)
            return rel;

        Node headerNode = template.getPersistentState(metaHeader);

        Node tagNode;
        try {
            tagNode = template.getNode(tag.getId());
        } catch (RuntimeException e) {
            logger.error("Weird error looking for tag [{}] with ID [{}]", tag.getKey(), tag.getId());
            throw (e);
        }
        //Primary exploration relationship
        Node start = (isReversed? headerNode:tagNode);
        Node end = (isReversed? tagNode:headerNode);

        Relationship r = template.getRelationshipBetween(start, end, relationshipName);

        if (r != null) {
            return rel;
        }
        template.createRelationshipBetween(start, end, relationshipName, propMap);
        logger.trace("Created Relationship Tag[{}] of type {}", tag, relationshipName);
        return rel;
    }

    @Autowired
    TagDao tagDao;

    @Override
    public void deleteAuditTags(MetaHeader metaHeader, Collection<TrackTag> trackTags) throws DatagioException {
        Node auditNode = null;
        for (TrackTag tag : trackTags) {
            if (!tag.getMetaId().equals(metaHeader.getId()))
                throw new DatagioException("Tags do not belong to the required MetaHeader");

            if (auditNode == null) {
                auditNode = template.getNode(tag.getMetaId());
            }

            Relationship r = template.getRelationship(tag.getId());
            r.delete();
            // ToDo - remove nodes that are not attached to other nodes.
            if ( ! r.getOtherNode(auditNode).getRelationships().iterator().hasNext() )
                template.getNode(tag.getTag().getId()).delete();
        }
    }

    /**
     * Rewrites the relationship type between the nodes copying the properties
     *
     * @param metaHeader track
     * @param existingTag current
     * @param newType     new type name
     */
    @Override
    public void changeType(MetaHeader metaHeader, TrackTag existingTag, String newType) {
        if (!relationshipExists(metaHeader, existingTag.getTag(), newType)) {
            Relationship r = template.getRelationship(existingTag.getId());
            Iterable<String> propertyKeys = r.getPropertyKeys();
            Map<String, Object> properties = new HashMap<>();
            for (String propertyKey : propertyKeys) {
                properties.put(propertyKey, r.getProperty(propertyKey));
            }
            template.createRelationshipBetween(r.getStartNode(), r.getEndNode(), newType, properties);
            r.delete();
        }
    }

    @Override
    public Set<MetaHeader> findTrackTags(Tag tag) {
        String query = "start tag=node({tagId}) " +
                "       match tag-[]->track" +
                "      return track";
        Map<String, Object> params = new HashMap<>();
        params.put("tagId", tag.getId());
        Result<Map<String, Object>> result = template.query(query, params);
        Set<MetaHeader> results = new HashSet<>();
        for (Map<String, Object> row : result) {
            MetaHeader audit = template.convert(row.get("track"), MetaHeaderNode.class);
            results.add(audit);
        }

        return results;
    }

    @Override
    public Boolean relationshipExists(MetaHeader metaHeader, Tag tag, String relationshipName) {
        Node end = template.getPersistentState(metaHeader);
        Node start = template.getNode(tag.getId());
        return (template.getRelationshipBetween(start, end, relationshipName) != null);

    }

    @Autowired
    EngineConfig engineAdmin;

    @Override
    public Set<TrackTag> getMetaTrackTagsOutbound(Company company, MetaHeader metaHeader) {
        Set<TrackTag> tagResults = new HashSet<>();
        if ( null == metaHeader.getId())
            return tagResults;
        String query = "match (track:MetaHeader)-[tagType]->(tag"+Tag.DEFAULT + engineAdmin.getTagSuffix(company) + ") " +
                "where id(track)={auditId} \n" +
                "optional match tag-[:located]-(located)-[*0..2]-(country:Country) \n" +
                "optional match located-[*0..2]->(state:State) " +
                "return tag,tagType,located,state, country";

        return getTrackTags(metaHeader, tagResults, query);

    }

    @Override
    public Set<TrackTag> getMetaTrackTags(Company company, MetaHeader metaHeader) {
        Set<TrackTag> tagResults = new HashSet<>();
        if ( null == metaHeader.getId())
            return tagResults;
        String query = "match (track:MetaHeader)-[tagType]-(tag" +Tag.DEFAULT+ engineAdmin.getTagSuffix(company) + ") " +
                "where id(track)={auditId} \n" +
                "optional match tag-[:located]-(located)-[*0..2]-(country:Country) \n" +
                "optional match located-[*0..2]->(state:State) " +
                "return tag,tagType,located,state, country";


        return getTrackTags(metaHeader, tagResults, query);
    }

    private Set<TrackTag> getTrackTags(MetaHeader metaHeader, Set<TrackTag> tagResults, String query) {
        Map<String, Object> params = new HashMap<>();
        params.put("auditId", metaHeader.getId());
        //Map<Long, TrackTag> tagResults = new HashMap<>();

        for (Map<String, Object> row : template.query(query, params)) {
            Node n = (Node) row.get("tag");
            TagNode tag = new TagNode(n);
            Relationship relationship = template.convert(row.get("tagType"), Relationship.class);
            TrackTagRelationship auditTag = new TrackTagRelationship(metaHeader, tag, relationship);

            Node loc = (Node) row.get("located");

            if (loc != null) {
                GeoData geoData = new GeoData();
                Node country = (Node) row.get("country");
                Node state = (Node) row.get("state");
                geoData.setCity((String) loc.getProperty("name"));

                if (country != null && country.hasProperty("name")) {
                    // ToDo: Needs to be a Country object, not a tag. Properties here don't make sense
                    geoData.setIsoCode((String) country.getProperty("name"));
                    geoData.setCountry((String) country.getProperty("code"));
                }
                if (state != null && state.hasProperty("name"))
                    geoData.setState((String) state.getProperty("name"));
                auditTag.setGeoData(geoData);
            }
            // Commenting out for DoubleCheck. Doesn't seem to serve any purpose in
            // search anyway
            auditTag.setWeight(null);
            tagResults.add(auditTag) ;
            //tagResults.put(tag.getId(), auditTag);
        }
        //return new HashSet<>(tagResults.values());
        return tagResults;
    }


}
