package com.auditbucket.client.rest;

import com.auditbucket.helper.DatagioException;

import java.util.Map;

/**
 * User: mike
 * Date: 23/07/14
 * Time: 12:28 PM
 */
public interface IStaticDataResolver {
    String resolveCountryISOFromName(String name) throws DatagioException;

    String resolve (String type, Map<String, String> args);
}
