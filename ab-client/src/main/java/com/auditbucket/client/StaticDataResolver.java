package com.auditbucket.client;

import com.auditbucket.helper.DatagioException;

/**
 * User: mike
 * Date: 27/04/14
 * Time: 1:03 PM
 */
public interface StaticDataResolver {
    String resolveCountryISOFromName(String name) throws DatagioException;
}
