package com.auditbucket.client.rest;

import com.auditbucket.helper.FlockException;

import java.util.Map;

/**
 * User: mike
 * Date: 27/04/14
 * Time: 1:03 PM
 */
public interface IStaticDataResolver {
    String resolveCountryISOFromName(String name) throws FlockException;

    String resolve (String type, Map<String, Object> args);
}
