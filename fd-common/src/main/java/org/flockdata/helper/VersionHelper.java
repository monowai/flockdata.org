/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.helper;

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
        String build = null;
        String plan = null;
        try {
            Properties p = new Properties();

            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
            if (is != null) {
                p.load(is);
                version = p.getProperty("version", "DEV");
                build = p.getProperty("build", "DEV");
                if (build.equals("${bambooBuildNumber}"))
                    build = "DEV";

                plan = p.getProperty("plan", "DEV");
                if ( plan.equals("${bambooPlan}"))
                    plan = "DEV";
            }
        } catch (Exception e) {
            // ignore
        }
        return version + " (" + plan + "/" + build +")";
    }
}
