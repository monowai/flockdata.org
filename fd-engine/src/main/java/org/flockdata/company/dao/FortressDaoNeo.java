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

package org.flockdata.company.dao;

import java.util.Collection;
import java.util.List;
import org.flockdata.data.Company;
import org.flockdata.data.Fortress;
import org.flockdata.data.Segment;
import org.flockdata.engine.configure.EngineConfig;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.data.graph.FortressSegmentNode;
import org.flockdata.engine.data.graph.FortressUserNode;
import org.flockdata.integration.IndexManager;
import org.flockdata.registration.FortressInputBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author mholdsworth
 * @tag Fortress, Neo4j,
 * @since 20/04/2013
 */
@Repository
public class FortressDaoNeo {
  @Autowired
  Neo4jTemplate template;
  @Autowired
  private FortressRepository fortressRepo;
  @Autowired
  private FortressSegmentRepository fortressSegmentRepo;
  @Autowired
  private FortressUserRepository fortressUserRepo;
  @Autowired
  private IndexManager indexManager;
  @Autowired
  private EngineConfig engineConfig;
  private Logger logger = LoggerFactory.getLogger(FortressDaoNeo.class);

  public FortressNode save(Company company, FortressInputBean fortressInput) {
    if (fortressInput.isSearchEnabled() == null) {
      fortressInput.setSearchEnabled(engineConfig.isSearchEnabled());
    }

    FortressNode fortress = new FortressNode(fortressInput, company);
    fortress.setRootIndex(indexManager.getIndexRoot(fortress));
    return fortressRepo.save(fortress);
  }

  public FortressNode findOne(Long fortressId) {
    return fortressRepo.findOne(fortressId);
  }

  @Cacheable(value = "fortressUser", unless = "#result==null")
  public FortressUserNode getFortressUser(Long fortressId, String name) {
    return fortressUserRepo.findBySchemaPropertyValue("key", fortressId + "." + name);
  }

  public List<FortressNode> findFortresses(Long companyID) {
    return fortressRepo.findCompanyFortresses(companyID);
  }

  public FortressUserNode findOneUser(Long fortressUserId) {
    return fortressUserRepo.findOne(fortressUserId);
  }

  public FortressUserNode save(FortressNode fortress, String fortressUserName) {
    return fortressUserRepo.save(new FortressUserNode(fortress, fortressUserName));
  }

  public String delete(Fortress fortress) {
    fortressRepo.delete((FortressNode) fortress);
    return "OK";
  }

  public FortressNode getFortressByName(Long companyId, String fortressName) {
    return fortressRepo.getFortressByName(companyId, fortressName);
  }

  public FortressNode getFortressByCode(Long companyId, String fortressCode) {
    return fortressRepo.getFortressByCode(companyId, fortressCode.toLowerCase());
  }

  public Segment saveSegment(Segment segment) {
    Segment result = findSegment(segment.getFortress(), segment.getKey());
    if (result == null) {
      result = fortressSegmentRepo.save((FortressSegmentNode) segment);
    }
    return result;
  }

  public Segment getDefaultSegment(Fortress fortress) {
    FortressSegmentNode segment = (FortressSegmentNode) fortress.getDefaultSegment();
    template.fetch(segment);
    return segment;
  }

  public Collection<Segment> getSegments(Fortress fortress) {
    return fortressSegmentRepo.findFortressSegments(fortress.getId());
  }

  Segment findSegment(Fortress fortress, String segmentKey) {
    return fortressSegmentRepo.findSegment(fortress.getId(), segmentKey);
  }

  public void purgeFortress(Long fortressId) {
    fortressSegmentRepo.purgeFortressSegments(fortressId);
  }

  public FortressNode update(FortressNode existing) {
    return template.save(existing);
  }

  public Fortress save(FortressNode fortress) {
    template.save(fortress);
    return fortress;
  }
}
