package com.auditbucket.registration.service;


import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.ICompanyUser;
import com.auditbucket.registration.model.ISystemUser;
import com.auditbucket.registration.repo.neo4j.model.Company;
import com.auditbucket.registration.repo.neo4j.model.CompanyUser;
import com.auditbucket.registration.repo.neo4j.model.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private SecurityHelper securityHelper;

    private static ISystemUser GUEST = new SystemUser("Guest", null, null, false);

    @Transactional
    public ISystemUser registerSystemUser(RegistrationBean regBean) {
        ISystemUser systemUser = systemUserService.findByName(regBean.getName());

        if (systemUser != null)
            throw new IllegalArgumentException("User [" + regBean.getName() + "] is already registered.");

        ICompany company = companyService.findByName(regBean.getCompanyName());
        if (company == null) {
            company = new Company(regBean.getCompanyName());
            company = companyService.save(company);
        }
        regBean.setCompany(company);
        systemUser = systemUserService.save(regBean);


        return systemUser;
    }

    @Transactional
    public ICompanyUser addCompanyUser(String companyUser, String companyName) {
        String systemUser = securityHelper.isValidUser();
        ICompany company = companyService.findByName(companyName);

        if (company == null) {
            throw new IllegalArgumentException("Company does not exist");
        }

        isAdminUser(company, "[" + systemUser + "] is not authorised to add company user records for [" + companyName + "]");

        ICompanyUser iCompanyUser = companyService.getCompanyUser(company, companyUser);
        if (iCompanyUser == null) {
            iCompanyUser = companyService.save(new CompanyUser(companyUser, company));
        }

        return iCompanyUser;
    }


    public ISystemUser isAdminUser(ICompany company, String message) {
        String systemUser = securityHelper.isValidUser();
        ISystemUser adminUser = companyService.getAdminUser(company, systemUser);
        if (adminUser == null)
            throw new IllegalArgumentException(message);
        return adminUser;
    }

    /**
     * @return currently logged-in ISystemUser or Guest if anonymous
     */
    public ISystemUser getSystemUser() {
        String systemUser = securityHelper.getUserName(false, false);
        if (systemUser == null)
            return GUEST;
        ISystemUser iSystemUser = systemUserService.findByName(systemUser);
        if (iSystemUser == null) {
            return GUEST;
        } else {
            return iSystemUser;
        }


    }

}
