package com.auditbucket.helper;

import com.auditbucket.authentication.handler.ApiKeyInterceptor;
import com.auditbucket.registration.model.Company;

import javax.servlet.http.HttpServletRequest;

/**
 * User: mike
 * Date: 28/08/14
 * Time: 10:27 AM
 */
public class CompanyResolver {
    public static Company resolveCompany(HttpServletRequest request) throws FlockException {
        Company company = (Company) request.getAttribute(ApiKeyInterceptor.COMPANY);
        if (company == null )
            throw new NotFoundException("Unable to identify any Company that you are authorised to work with");
        return company;
    }
}
