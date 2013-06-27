package com.auditbucket.registration.dao;

import com.auditbucket.registration.model.ISystem;

/**
 * User: mike
 * Date: 26/06/13
 * Time: 8:12 PM
 */
public interface SystemDaoI {
    ISystem save(ISystem system);

    ISystem findOne(String name);
}
