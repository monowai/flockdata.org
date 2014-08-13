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

import java.io.InputStream;
import java.util.Properties;

/**
 * User: Mike Holdsworth
 * Since: 29/08/13
 */
public class VersionHelper {
    public static String getABVersion() {
        String path = "/version.properties";

        String version = null;
        try {
            Properties p = new Properties();

            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
            if (is != null) {
                p.load(is);
                version = p.getProperty("version", "DEV");
            }
        } catch (Exception e) {
            // ignore
        }
        return version;
    }
}
