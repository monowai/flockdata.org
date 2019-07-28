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

package org.flockdata.engine.track.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.flockdata.data.Company;
import org.flockdata.data.Document;
import org.flockdata.data.Fortress;
import org.flockdata.data.Segment;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.matrix.MatrixResults;
import org.flockdata.helper.FlockException;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityKeyBean;
import org.flockdata.track.bean.TrackResultBean;

/**
 * @author mholdsworth
 * @since 20/06/2015
 */
public interface ConceptService {

  Collection<DocumentResultBean> getDocumentsInUse(Company company);

  Set<DocumentResultBean> findConcepts(Company company, String documentName, boolean withRelationships);

  Set<DocumentResultBean> findConcepts(Company company, Collection<String> documentNames, boolean withRelationships);

  DocumentNode resolveByDocCode(Fortress fortress, String documentCode);

  DocumentNode resolveByDocCode(Fortress fortress, String documentCode, Boolean createIfMissing);

  void registerConcepts(Iterable<TrackResultBean> resultBeans);

  void linkEntities(DocumentNode sourceType, DocumentNode targetType, EntityKeyBean entityKeyBean) throws FlockException;

  DocumentNode save(DocumentNode documentType);

  DocumentNode findDocumentType(Fortress fortress, String documentName);

  DocumentNode findDocumentType(Fortress fortress, String documentName, boolean createIfMissing);

  DocumentNode findOrCreate(Fortress fortress, DocumentNode documentType);

  Set<DocumentResultBean> getConceptsWithRelationships(Company company, Collection<String> documents);

  Collection<DocumentResultBean> getDocumentsInUse(Company fdCompany, Collection<String> fortresses) throws FlockException;

  Collection<DocumentResultBean> getDocumentsInUse(Company fdCompany, String fortress) throws FlockException;

  Collection<DocumentNode> makeDocTypes(Segment segment, List<EntityInputBean> inputBeans) throws FlockException;

  void delete(Document documentType);

  DocumentNode findDocumentTypeWithSegments(DocumentNode documentType);

  DocumentResultBean findDocumentTypeWithSegments(FortressNode f, String doc);

  void delete(Document documentType, Segment segment);

  /**
   * Concept structure associated to a Fortress. All DocumentTypes and connected concepts
   *
   * @param company  org that owns the fortress
   * @param fortress that which we are interested in
   * @return edges and nodes
   */
  MatrixResults getContentStructure(Company company, String fortress);

  Map<String, DocumentResultBean> getParents(Document documentType);
}
