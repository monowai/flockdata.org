package com.auditbucket.registration.repo.neo4j;

import com.auditbucket.registration.model.IFortressUser;
import com.auditbucket.registration.repo.neo4j.model.FortressUser;
import com.auditbucket.registration.repo.neo4j.model.SystemUser;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

public interface SystemUserRepository extends GraphRepository<SystemUser> {

    @Query(elementClass = FortressUser.class, value = "start sysUser=node:sysUserName(name={0}) match sysUser-[:administers]->company-[:owns]->fortress<-[:fortressUser]-fortressUser where fortressUser.name ={2} and fortress.name={1} return fortressUser")
    IFortressUser getFortressUser(String userName, String fortressName, String fortressUser);


}
