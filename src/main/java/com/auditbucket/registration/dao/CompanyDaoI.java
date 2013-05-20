package com.auditbucket.registration.dao;

import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.ICompanyUser;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.ISystemUser;

/**
 * User: mike
 * Date: 20/04/13
 * Time: 6:31 PM
 */
public interface CompanyDaoI {
    public ICompany save(ICompany systemUser);

    public ICompanyUser save(ICompanyUser companyUser);

    public ICompany findByPropertyValue(String name, Object value);

    public ICompanyUser getCompanyUser(Long id, String userName);

    public IFortress getFortress(Long id, String fortressName);

    public ISystemUser getAdminUser(Long id, String name);

    public Iterable<ICompanyUser> getCompanyUsers(String companyName);
}
