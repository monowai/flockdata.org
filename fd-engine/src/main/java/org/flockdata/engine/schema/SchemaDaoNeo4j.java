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

package org.flockdata.engine.schema;

import org.flockdata.engine.concept.dao.ConceptDaoNeo;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Fortress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Maintains company specific Schema details. Structure of the nodes that FD has established
 * based on Entities, DocumentTypes, Tags and Relationships
 * <p>
 * User: mike
 * Date: 3/04/14
 * Time: 7:30 AM
 */
@Repository
public class SchemaDaoNeo4j {

    @Autowired
    ConceptDaoNeo conceptDaoNeo4j;

    @Autowired
    Neo4jTemplate template;

    private Logger logger = LoggerFactory.getLogger(SchemaDaoNeo4j.class);

    public Boolean ensureSystemConstraints(Company company) {
        logger.debug("Creating system constraints for {} ", company.getName());
        template.query("create constraint on (t:Country) assert t.key is unique", null);
        template.query("create constraint on (t:CountryAlias) assert t.key is unique", null);
        template.query("create constraint on (t:State) assert t.key is unique", null);
        template.query("create constraint on (t:StateAlias) assert t.key is unique", null);
        // ToDo: Create a city node. The key should be country.{state}.city
        //template.query("create constraint on (t:City) assert t.key is unique", null);
        logger.debug("Created system constraints");
        return true;
    }


    /**
     * Make sure a unique index exists for the tag
     * Being a schema alteration function this is synchronised to avoid concurrent modifications
     *
     * @param tagPayload collection to process
     */
    @Transactional
    @Async
    public Future<Boolean> ensureUniqueIndexes(Collection<TagInputBean> tagPayload) {
        Collection<String> knownLabels = getAllLabels();
        Collection<String> labels = getLabelsToCreate(tagPayload, knownLabels);
        int size = labels.size();

        if (size > 0) {
            logger.debug("Made " + size + " constraints");
            for (String label : labels) {
                boolean quoted = label.contains(" ") || label.contains("/");

                String cLabel = quoted ?"`"+label: label;

                template.query("create constraint on (t:" + cLabel + (quoted? "'":"") +") assert t.key is unique", null);
                template.query("create constraint on (t:" + cLabel + "Alias "+ (quoted?"'":"")+") assert t.key is unique", null);

            }

        }
        logger.debug("No label constraints required");

        return new AsyncResult<>(Boolean.TRUE);
    }

    private Collection<String> getAllLabels() {
        return template.getGraphDatabase().getAllLabelNames();
    }

    private Collection<String> getLabelsToCreate(Iterable<TagInputBean> tagInputs, Collection<String> knownLabels) {
        Collection<String> toCreate = new ArrayList<>();
        for (TagInputBean tagInput : tagInputs) {
            if (tagInput != null) {
                logger.trace("Checking label for {}", tagInput);
                String label = tagInput.getLabel();
                if (!knownLabels.contains(label) && !toCreate.contains(label)) {
                    if (!(tagInput.isDefault() || isSystemLabel(tagInput.getLabel()))) {
                        logger.debug("Calculated candidate label index for [" + tagInput.getLabel() + "]");
                        toCreate.add(tagInput.getLabel());
                        knownLabels.add(tagInput.getLabel());
                    }
                }
                if (!tagInput.getTargets().isEmpty()) {
                    tagInput.getTargets()
                            .keySet()
                            .stream()
                            .filter(key
                                    -> key != null)
                            .forEach(key
                                    -> toCreate.addAll(getLabelsToCreate(tagInput.getTargets().get(key), knownLabels)));
                }
            } else
                logger.debug("Why is this null?");

        }
        return toCreate;

    }

    @Transactional
    public void purge(Fortress fortress) {

        String docRlx = "match (fort:Fortress)-[fd:FORTRESS_DOC]-(a:DocType)-[dr]-(o)-[k]-(p)" +
                "where id(fort)={fortId}  delete dr, k, o, fd;";

        // ToDo: Purge Unused Concepts!!
        HashMap<String, Object> params = new HashMap<>();
        params.put("fortId", fortress.getId());
        template.query(docRlx, params);
    }

    public static boolean isSystemLabel(String index) {
        return (index.equals("Country") || index.equals("City"));
    }


    @Transactional
    public void waitForIndexes() {
        template.getGraphDatabaseService().schema().awaitIndexesOnline(6000, TimeUnit.MILLISECONDS);
    }

}
