package com.auditbucket.registration.repo.neo4j;

import com.auditbucket.registration.repo.neo4j.model.FortressUser;
import com.auditbucket.registration.repo.neo4j.model.SystemUser;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

public interface FortressUserRepository extends GraphRepository<FortressUser> {

    @Query(value = "start fortress=node({0}) match fortress<-[r:fortressUser]-fUser where fUser.name ={1} return fUser")
    SystemUser getAdminUser(long fortressId, String userName);

}
