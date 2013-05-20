package com.auditbucket.registration.repo.neo4j.dao;

import com.auditbucket.registration.dao.CompanyDaoI;
import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.ICompanyUser;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.ISystemUser;
import com.auditbucket.registration.repo.neo4j.CompanyRepository;
import com.auditbucket.registration.repo.neo4j.CompanyUserRepository;
import com.auditbucket.registration.repo.neo4j.model.Company;
import com.auditbucket.registration.repo.neo4j.model.CompanyUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * User: mike
 * Date: 20/04/13
 * Time: 10:05 PM
 */
@Repository
public class CompanyDaoImpl implements CompanyDaoI {
    @Autowired
    private CompanyRepository companyRepo;

    @Autowired
    private CompanyUserRepository companyUserRepo;


    @Override
    public ICompany save(ICompany company) {
        return companyRepo.save((Company) company);  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ICompanyUser save(ICompanyUser companyUser) {
        return companyUserRepo.save((CompanyUser) companyUser);  //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public ICompany findByPropertyValue(String name, Object value) {
        return companyRepo.findByPropertyValue(name, value);
    }

    @Override
    public ICompanyUser getCompanyUser(Long id, String userName) {
        return companyRepo.getCompanyUser(id, userName);
    }

    @Override
    public IFortress getFortress(Long companyId, String fortressName) {
        return companyRepo.getFortress(companyId, fortressName);
    }

    @Override
    public ISystemUser getAdminUser(Long id, String name) {
        return companyRepo.getAdminUser(id, name);
    }

    @Override
    public Iterable<ICompanyUser> getCompanyUsers(String companyName) {
        return companyRepo.getCompanyUsers(companyName);
    }
}
