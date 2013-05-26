package com.auditbucket.registration.repo.neo4j;

import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.repo.neo4j.model.Fortress;
import com.auditbucket.registration.repo.neo4j.model.FortressUser;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.List;


public interface FortressRepository extends GraphRepository<Fortress> {

    @Query(value = "start fortress=node({0}) match fortress-[:fortressUser]->fu where fu.name = {1} return fu")
    FortressUser getFortressUser(Long fortressId, String userName);

    @Query(elementClass = Fortress.class, value = "start company=node({0}) match company-[:owns]->f return f")
    List<IFortress> findCompanyFortresses(Long companyID);

}
