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

package com.auditbucket.registration.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Generates unique keys based on various algorithms.
 *
 * User: Mike Holdsworth
 * Since: 11/09/13
 */
@Service
public class KeyGenService {
    public enum METHOD {
        UUID, SNOWFLAKE, BASE64
    }

    public String getUniqueKey() {
        return getUniqueKey(METHOD.BASE64);
    }

    String getUniqueKey(METHOD method) {
        // Snowflake?
        if (method.equals(METHOD.SNOWFLAKE))
            return getSnowFlake();
        else if (method.equals(METHOD.BASE64))
            return Base64.format(UUID.randomUUID());
        else

        return getUUID();
    }

    //ToDo: Implement!
    private String getSnowFlake() {
        return getUUID();
    }

    private String getUUID() {
        return UUID.randomUUID().toString();
    }



}
