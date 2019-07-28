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

package org.flockdata.graph.schema;

import java.util.ArrayList;
import java.util.Collection;
import org.flockdata.data.Fortress;
import org.flockdata.graph.model.CompanyNode;
import org.flockdata.helper.TagHelper;
import org.flockdata.registration.TagInputBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author mholdsworth
 * @tag Administration, Neo4j, Index
 * @since 16/06/2014
 */
@Service
public class SchemaServiceNeo4j implements SchemaService {

  private static Logger logger = LoggerFactory.getLogger(SchemaServiceNeo4j.class);
  private final SchemaDaoNeo4j schemaDao;

  @Autowired
  public SchemaServiceNeo4j(SchemaDaoNeo4j schemaDao) {
    this.schemaDao = schemaDao;
  }

  public Boolean ensureSystemIndexes(CompanyNode company) {
    return schemaDao.ensureSystemConstraints(company);
  }

  @Override
  public void purge(Fortress fortress) {
    schemaDao.purge(fortress);
  }

  /**
   * Ensures unique indexes exist for the payload
   * <p>
   * Being a schema alteration function this is synchronised to avoid concurrent modifications
   *
   * @param tagPayload collection to process
   */
  @Override
//    @Async("fd-track")
  public Boolean ensureUniqueIndexes(Collection<TagInputBean> tagPayload) {

    Collection<String> knownLabels = schemaDao.getAllLabels();
    Collection<String> labels = getLabelsToCreate(tagPayload, knownLabels);
    int size = labels.size();

    if (size > 0) {
      logger.debug("Made " + size + " constraints");
      labels.forEach(schemaDao::ensureUniqueIndex);

    } else {
      logger.debug("No label constraints required");
    }
    return Boolean.TRUE;
  }

  private Collection<String> getLabelsToCreate(Iterable<TagInputBean> tagInputs, Collection<String> knownLabels) {
    Collection<String> toCreate = new ArrayList<>();
    for (TagInputBean tagInput : tagInputs) {
      if (tagInput != null) {
        logger.trace("Checking label for {}", tagInput);
        String label = tagInput.getLabel();
        if (!knownLabels.contains(label) && !toCreate.contains(label)) {
          if (!(tagInput.isDefault() || TagHelper.isSystemLabel(tagInput.getLabel()))) {
            logger.debug("Calculated candidate label index for [" + tagInput.getLabel() + "]");
            toCreate.add(tagInput.getLabel());
            knownLabels.add(tagInput.getLabel());
          }
        }
        if (tagInput.hasTargets()) {
          tagInput.getTargets()
              .keySet()
              .stream()
              .filter(key
                  -> key != null)
              .forEach(key
                  -> toCreate.addAll(getLabelsToCreate(tagInput.getTargets().get(key), knownLabels)));
        }
      } else {
        logger.debug("Why is this null?");
      }

    }
    return toCreate;

  }

}
