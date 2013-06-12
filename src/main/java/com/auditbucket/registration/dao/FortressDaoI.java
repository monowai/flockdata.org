package com.auditbucket.registration.dao;

import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.IFortressUser;

import java.util.List;

/**
 * User: mike
 * Date: 20/04/13
 * Time: 6:31 PM
 */
public interface FortressDaoI {
    public IFortress save(IFortress fortress);

    public IFortress findByPropertyValue(String name, Object value);

    public IFortress findOne(Long id);

    public IFortressUser getFortressUser(Long id, String name);

    List<IFortress> findFortresses(Long companyID);

    IFortressUser findOneUser(Long id);

    IFortressUser save(IFortressUser fortressUser);
}
