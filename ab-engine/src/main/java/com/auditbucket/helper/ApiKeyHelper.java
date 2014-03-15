package com.auditbucket.helper;

/**
 * API key precedence
 *
 * User: mike
 * Date: 15/03/14
 * Time: 11:51 AM
 */
public class ApiKeyHelper {
    /**
     *
     * headerParam overrides requestParam
     *
     * @param requestParam requestParam
     * @param headerParam    headerParam
     * @return null or param.
     */
    public static String resolveKey (String requestParam, String headerParam){
        String key = requestParam;
        if (headerParam != null)
            key = headerParam;
        return key;


    }

}
