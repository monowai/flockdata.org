package com.auditbucket.registration.dao;

import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.ISystemUser;

/**
 * User: mike
 * Date: 20/04/13
 * Time: 6:31 PM
 */
public interface RegistrationDaoI {
    public ISystemUser save(ISystemUser systemUser);

    public ISystemUser findByPropertyValue(String name, Object value);

    ISystemUser save(ICompany company, String userName, String password);
}
