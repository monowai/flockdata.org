package com.auditbucket.registration.repo.neo4j.dao;

import com.auditbucket.registration.dao.SystemDaoI;
import com.auditbucket.registration.model.ISystem;
import com.auditbucket.registration.repo.neo4j.SystemRepository;
import com.auditbucket.registration.repo.neo4j.model.SystemId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * User: mike
 * Date: 26/06/13
 * Time: 8:33 PM
 */
@Repository
public class SystemDaoImpl implements SystemDaoI {

    @Autowired
    SystemRepository sysRepo;

    public ISystem save(ISystem system) {
        return sysRepo.save((SystemId) system);
    }

    @Override
    public ISystem findOne(String name) {
        return null;
    }
}
