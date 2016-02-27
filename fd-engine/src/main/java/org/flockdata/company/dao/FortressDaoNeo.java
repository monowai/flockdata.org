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

package org.flockdata.company.dao;

import org.flockdata.model.Company;
import org.flockdata.model.Fortress;
import org.flockdata.model.FortressSegment;
import org.flockdata.model.FortressUser;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.shared.IndexManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * User: Mike Holdsworth
 * Date: 20/04/13
 * Time: 10:29 PM
 */
@Repository
public class FortressDaoNeo  {
    @Autowired
    private FortressRepository fortressRepo;

    @Autowired
    private FortressSegmentRepository fortressSegmentRepo;

    @Autowired
    private FortressUserRepository fortressUserRepo;

    @Autowired
    private IndexManager indexHelper;

    @Autowired
    Neo4jTemplate template;

    private Logger logger = LoggerFactory.getLogger(FortressDaoNeo.class);

    public Fortress save(Company company, FortressInputBean fortressInput) {
        Fortress fortress = new Fortress(fortressInput, company);
        fortress.setRootIndex(indexHelper.getIndexRoot(fortress));
        return fortressRepo.save(fortress);
    }

    public Fortress findOne(Long fortressId) {
        return fortressRepo.findOne(fortressId);
    }

    @Cacheable(value = "fortressUser", unless = "#result==null")
    public FortressUser getFortressUser(Long fortressId, String name) {
        return fortressUserRepo.findBySchemaPropertyValue("key", fortressId + "." + name);
    }

    public List<Fortress> findFortresses(Long companyID) {
        return fortressRepo.findCompanyFortresses(companyID);
    }

    public FortressUser findOneUser(Long fortressUserId) {
        return fortressUserRepo.findOne(fortressUserId);
    }

    public FortressUser save(Fortress fortress, String fortressUserName) {
        return fortressUserRepo.save(new FortressUser(fortress, fortressUserName));
    }

    public String delete(Fortress fortress) {
        fortressRepo.delete(fortress);
        return "OK";
    }

    public Fortress getFortressByName(Long companyId, String fortressName) {
        return fortressRepo.getFortressByName(companyId, fortressName);
    }

    public Fortress getFortressByCode(Long companyId, String fortressCode) {
        return fortressRepo.getFortressByCode(companyId, fortressCode.toLowerCase());
    }

    public FortressSegment saveSegment(FortressSegment segment){
        FortressSegment result= findSegment(segment.getFortress(), segment.getKey());
        if ( result == null )
            result = fortressSegmentRepo.save(segment);
        return result;
    }

    public FortressSegment getDefaultSegement(Fortress fortress){
        FortressSegment segment = fortress.getDefaultSegment();
        template.fetch(segment);
        return segment;
    }

    public Collection<FortressSegment> getSegments(Fortress fortress) {
        return fortressSegmentRepo.findFortressSegments(fortress.getId()) ;
    }

    FortressSegment findSegment(Fortress fortress, String segmentKey){
        return fortressSegmentRepo.findSegment(fortress.getId(),segmentKey);
    }
}
