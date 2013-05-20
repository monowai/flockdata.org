package com.auditbucket.registration.repo.neo4j.dao;

import com.auditbucket.registration.dao.RegistrationDaoI;
import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.IFortressUser;
import com.auditbucket.registration.model.ISystemUser;
import com.auditbucket.registration.repo.neo4j.SystemUserRepository;
import com.auditbucket.registration.repo.neo4j.model.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * User: mike
 * Date: 20/04/13
 * Time: 6:40 PM
 */
@Repository
public class RegistrationDaoImpl implements RegistrationDaoI {
    @Autowired
    private
    SystemUserRepository suRepo;

    @Override
    public ISystemUser save(ISystemUser systemUser) {
        return suRepo.save((SystemUser) systemUser);
    }

    public ISystemUser findByPropertyValue(String name, Object value) {
        return suRepo.findByPropertyValue(name, value);
    }

    @Override
    public ISystemUser save(ICompany company, String userName, String password) {
        SystemUser su = new SystemUser(userName, password, company, true);
        return save(su);
    }

    public IFortressUser getFortressUser(String userName, String fortressName, String fortressUser) {
        return suRepo.getFortressUser(userName, fortressName, fortressUser);
    }
}
