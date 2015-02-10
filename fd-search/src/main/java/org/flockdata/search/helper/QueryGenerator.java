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

package org.flockdata.search.helper;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: macpro
 * Date: 04/08/2014
 * Time: 15:28
 * To change this template use File | Settings | File Templates.
 */
public class QueryGenerator {
    private static Logger logger = LoggerFactory.getLogger(QueryGenerator.class);

    public static String getSimpleQuery(String queryString, Boolean highlightEnabled) {
        logger.debug("getSimpleQuery {}", queryString);
        StringBuilder simpleQuery = new StringBuilder();
        if ( queryString.contains("\"")) {
            queryString = StringEscapeUtils.escapeJson(queryString);
        }

        simpleQuery.append("{ \"query\": {"
                + "        \"query_string\": { "
                + "            \"query\": " + '"').append(queryString).append('"')
                .append("          }")
                .append("  }");

        if (highlightEnabled) {
            simpleQuery.append(",\n" +
                    "  \"highlight\": { " +
                    "\"pre_tags\" : [\"<strong>\"]," +
                    "\"post_tags\" : [\"</strong>\"]," +
                    "\"encoder\" : \"html\"," +
                    "    \"fields\": { " +
                    "      \"*\": {} " +
                    "    } " +
                    "  }");
        }
        simpleQuery.append(" }");
        return simpleQuery.toString();
    }
}
