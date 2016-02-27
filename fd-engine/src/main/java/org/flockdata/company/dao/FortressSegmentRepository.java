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

import org.flockdata.model.FortressSegment;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Collection;

/**
 * Created by mike on 13/10/15.
 */
public interface FortressSegmentRepository extends GraphRepository<FortressSegment> {

    @Query( value =  "match (fortress:Fortress)-[r:DEFINES]- (segments:FortressSegment) " +
            "where id(fortress) = {0} return segments")
    Collection<FortressSegment> findFortressSegments(Long id);

    @Query( value =  "match (fortress:Fortress)-[r:DEFINES]- (segment:FortressSegment) " +
            "where id(fortress) = {0} and segment.key = {1} return segment")
    FortressSegment findSegment(Long id, String segmentName);
}
