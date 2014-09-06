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

import com.auditbucket.engine.repo.neo4j.model.MetaHeaderNode;
import com.auditbucket.engine.repo.neo4j.model.TagNode;
import com.auditbucket.engine.repo.neo4j.model.TrackTagRelationship;
import com.auditbucket.engine.service.EngineConfig;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.track.model.GeoData;
import com.auditbucket.track.model.Log;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.TrackTag;
import com.auditbucket.track.service.TagService;
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
@Repository("trackTagDAO")
public class TrackTagDaoNeo {
    @Autowired
    Neo4jTemplate template;

    @Autowired
    TagService tagService;

    @Autowired
    EngineConfig engineAdmin;

    private Logger logger = LoggerFactory.getLogger(TrackTagDaoNeo.class);

    static final String AB_WHEN = "abWhen";

    public TrackTag save(MetaHeader metaHeader, Tag tag, String relationshipName) {
        return save(metaHeader, tag, relationshipName, false, null);
    }

    public TrackTag save(MetaHeader ah, Tag tag, String metaLink, boolean reverse) {
        return save(ah, tag, metaLink, reverse, null );
    }

    /**
     * creates the relationship between the metaHeader and the tag of the name type.
     * If metaId== null, then an TrackTag for the caller to deal with otherwise the relationship
     * is persisted and null is returned.
     *
     *
     * @param metaHeader        constructed metaHeader
     * @param tag               tag
     * @param relationshipName  name
     * @param isReversed        tag<-header (false) or header->tag (true)
     * @param propMap           properties to associate with the relationship
     *
     * @return Null or TrackTag
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

    public void deleteTrackTags(MetaHeader metaHeader, Collection<TrackTag> trackTags) throws DatagioException {
        Node headerNode = null;
        for (TrackTag tag : trackTags) {
            if (!tag.getPrimaryKey().equals(metaHeader.getId()))
                throw new DatagioException("Tags do not belong to the required MetaHeader");

            if (headerNode == null) {
                headerNode = template.getNode(tag.getPrimaryKey());
            }

            Relationship r = template.getRelationship(tag.getId());
            r.delete();
            // ToDo - remove nodes that are not attached to other nodes.
            if ( ! r.getOtherNode(headerNode).getRelationships().iterator().hasNext() )
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

    /**
     * Moves the trackTag relationships from the MetaHeader to the Log
     * Purpose is to track at which version of a log the metadata covered2
     *
     * @param log pointer to the node we want to move the relationships to
     * @param metaHeader where the tags are currently located
     */
    public void moveTags(MetaHeader metaHeader, Log log, Collection<TrackTag> trackTags) {
        if ( log == null )
            return;

        Node logNode = template.getPersistentState(log);
        for (TrackTag trackTag : trackTags) {
            Node tagNode = template.getNode(trackTag.getTag().getId());

            Relationship relationship = template.getRelationship(trackTag.getId());
            if ( relationship!=null ){

                boolean isReversed = relationship.getStartNode().getId() == tagNode.getId();
                Node start = (isReversed? logNode :tagNode);
                Node end = (isReversed? tagNode: logNode);

                Map<String, Object> rlxProps = getRelationshipProperties(relationship);
                // Relationships are immutable, so we have to destroy and recreate
                template.delete(relationship);
                template.createRelationshipBetween(start, end, relationship.getType().name(), rlxProps);
            }
        }

    }

    /**
     * This version is used to relocate the tags associated with Log back to the MetaHeader
     *
     * This will examine the TrackTagDao.AB_WHEN property and >= fortressDate log when, it will be removed
     *
     * @param company       a validated company that the caller is allowed to work with
     * @param logToMoveFrom where the logs are currently associated
     * @param metaHeader    header to relocate them to
     */
    public void moveTags(Company company, Log logToMoveFrom, MetaHeader metaHeader) {
        if ( logToMoveFrom == null )
            return;

        Set<TrackTag> metaTags = getMetaTrackTags(company, metaHeader);
        Set<TrackTag> trackTags = findLogTags(company, logToMoveFrom);
        Node headerNode = template.getPersistentState(metaHeader);

        for (TrackTag trackTag : metaTags) {
            // Remove any MetaTags that are newer than the log being re-instated as the "current" truth
            // if trackTag.abWhen moreRecentThan logToMoveFrom

            Long metaWhen = (Long) trackTag.getProperties().get(AB_WHEN);
            template.fetch(logToMoveFrom.getTrackLog());
            logger.trace("MoveTags - Comparing {} with {}", metaWhen, logToMoveFrom.getTrackLog().getFortressWhen());
            if ( metaWhen.compareTo(logToMoveFrom.getTrackLog().getFortressWhen()) >= 0 ){
                // This tag was added to the metaHeader by a more recent log
                logger.trace("Removing {}", trackTag.getTag().getName());
                Relationship r = template.getRelationship(trackTag.getId());
                if ( r!=null )
                    template.delete(r);

            }
        }

        for (TrackTag trackTag : trackTags) {
            Node tagNode = template.getNode(trackTag.getTag().getId());

            Relationship relationship = template.getRelationship(trackTag.getId());
            if ( relationship!=null ){

                boolean isReversed = relationship.getStartNode().getId() == tagNode.getId();
                Node start = (isReversed? headerNode :tagNode);
                Node end = (isReversed? tagNode: headerNode);

                Map<String, Object> rlxProps = getRelationshipProperties(relationship);
                // Relationships are immutable, so we have to destroy and recreate
                template.delete(relationship);
                template.createRelationshipBetween(start, end, relationship.getType().name(), rlxProps);
            }
        }

    }

    private Map<String, Object> getRelationshipProperties(Relationship relationship) {
        Map<String, Object>rlxProps= new HashMap<>() ;
        for (String key : relationship.getPropertyKeys()) {
            // ToDo: System property checks
            rlxProps.put(key, relationship.getProperty(key));
        }
        return rlxProps;
    }

    public Set<MetaHeader> findTrackTags(Tag tag) {
        String query = "start tag=node({tagId}) " +
                "       match tag-[]->track" +
                "      return track";
        Map<String, Object> params = new HashMap<>();
        params.put("tagId", tag.getId());
        Result<Map<String, Object>> result = template.query(query, params);
        Set<MetaHeader> results = new HashSet<>();
        for (Map<String, Object> row : result) {
            MetaHeader header = template.convert(row.get("track"), MetaHeaderNode.class);
            results.add(header);
        }

        return results;
    }

    public Boolean relationshipExists(MetaHeader metaHeader, Tag tag, String relationshipName) {
        Node end = template.getPersistentState(metaHeader);
        Node start = template.getNode(tag.getId());
        return (template.getRelationshipBetween(start, end, relationshipName) != null);

    }

    public Set<TrackTag> findLogTags(Company company, Log log) {
        String query;
        if ("".equals(engineAdmin.getTagSuffix(company)))
            query = "match (log:_Log)-[tagType]-(tag:_Tag) where id(log)={logId} return tag, tagType";
        else
            query = "match (log:_Log)-[tagType]-(tag:_Tag" + engineAdmin.getTagSuffix(company) + ") where id(log)={logId} return tag, tagType";

        Map<String,Object>params = new HashMap<>();
        params.put("logId", log.getId());

        Result<Map<String, Object>> results = template.query(query, params);
        return getTrackTags(log.getId(), results);

    }

    public Set<TrackTag> getDirectedMetaTags(Company company, MetaHeader metaHeader, boolean outbound) {

        String tagDirection = "-[tagType]->";
        if ( !outbound )
            tagDirection = "<-[tagType]-";

        Set<TrackTag> tagResults = new HashSet<>();
        if ( null == metaHeader.getId())
            return tagResults;
        String query = "match (track:MetaHeader)"+tagDirection+"(tag"+Tag.DEFAULT + engineAdmin.getTagSuffix(company) + ") " +
                "where id(track)={id} \n" +
                "optional match tag-[:located]-(located)-[*0..2]-(country:Country) \n" +
                "optional match located-[*0..2]->(state:State) " +
                "return tag,tagType,located,state, country";

        return getTrackTags(metaHeader.getId(), query);

    }

    public Set<TrackTag> getMetaTrackTags(Company company, MetaHeader metaHeader) {
        Set<TrackTag> tagResults = new HashSet<>();
        if ( null == metaHeader.getId())
            return tagResults;
        String query = "match (track:MetaHeader)-[tagType]-(tag" +Tag.DEFAULT+ engineAdmin.getTagSuffix(company) + ") " +
                "where id(track)={id} \n" +
                "optional match tag-[:located]-(located)-[*0..2]-(country:Country) \n" +
                "optional match located-[*0..2]->(state:State) " +
                "return tag,tagType,located,state, country";


        return getTrackTags(metaHeader.getId(), query);
    }

    private Set<TrackTag> getTrackTags(Long primaryKey, String query) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", primaryKey);
        Result<Map<String, Object>> queryResults = template.query(query, params);
        return getTrackTags(primaryKey, queryResults);
    }

    private Set<TrackTag> getTrackTags(Long primaryKey, Result<Map<String, Object>> queryResults) {
        Set<TrackTag> tagResults = new HashSet<>();
        for (Map<String, Object> row : queryResults) {
            Node n = (Node) row.get("tag");
            TagNode tag = new TagNode(n);
            Relationship relationship = template.convert(row.get("tagType"), Relationship.class);
            TrackTagRelationship trackTag = new TrackTagRelationship(primaryKey, tag, relationship);

            Node loc = (Node) row.get("located");

            if (loc != null) {
                GeoData geoData = new GeoData();
                Node country = (Node) row.get("country");
                Node state = (Node) row.get("state");
                geoData.setCity((String) loc.getProperty("name"));

                if (country != null && country.hasProperty("name")) {
                    // ToDo: Needs to be a Country object
                    geoData.setIsoCode((String) country.getProperty("code"));
                    geoData.setCountry((String) country.getProperty("name"));
                }
                if (state != null && state.hasProperty("name"))
                    geoData.setState((String) state.getProperty("name"));
                trackTag.setGeoData(geoData);
            }
            tagResults.add(trackTag) ;
        }
        return tagResults;

    }


}
