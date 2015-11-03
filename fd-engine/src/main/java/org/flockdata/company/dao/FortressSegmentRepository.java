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
