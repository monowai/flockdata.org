package com.auditbucket.registration.service;


import com.auditbucket.registration.dao.CompanyDaoI;
import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.ICompanyUser;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.ISystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CompanyService {


    @Autowired
    private CompanyDaoI companyDao;


    public ICompany findByName(String companyName) {
        return companyDao.findByPropertyValue("name", companyName);
    }

    public ICompanyUser getCompanyUser(ICompany company, String userName) {
        return companyDao.getCompanyUser(company.getId(), userName);

    }

    public ICompanyUser getCompanyUser(String companyName, String userName) {
        ICompany company = findByName(companyName);
        if (company == null)
            return null;
        return companyDao.getCompanyUser(company.getId(), userName);

    }

    public IFortress getCompanyFortress(ICompany company, String fortressName) {
        return companyDao.getFortress(company.getId(), fortressName);
    }


    public ICompany save(ICompany company) {
        return companyDao.save(company);
    }


    public Iterable<ICompanyUser> getUsers(String companyName) {
        return companyDao.getCompanyUsers(companyName);
    }


    public IFortress getFortress(ICompany company, String name) {
        return companyDao.getFortress(company.getId(), name);
    }

    public ISystemUser getAdminUser(ICompany company, String name) {
        return companyDao.getAdminUser(company.getId(), name);
    }


    public ICompanyUser save(ICompanyUser companyUser) {
        return companyDao.save(companyUser);
    }

}
