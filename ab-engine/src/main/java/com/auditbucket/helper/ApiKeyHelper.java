/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

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
     * api key in the HttpHeader overrides one in the request
     *
     *
     * @param headerKey    headerKey
     * @param requestKey requestKey
     * @return null or param.
     */
    public static String resolveKey(String headerKey, String requestKey){
        String key = requestKey;
        if (headerKey !=null && (headerKey.startsWith("{{") && headerKey.endsWith("}}"))) // Postman "not-set" value
            headerKey = null;
        if (headerKey != null)
            key = headerKey;
        return key;
    }
}
