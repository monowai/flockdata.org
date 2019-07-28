/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.search.helper;

import org.apache.commons.lang3.StringEscapeUtils;
import org.flockdata.helper.JsonUtils;
import org.flockdata.search.QueryInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mholdsworth
 * @since 04/08/2014
 */
public class QueryGenerator {
  private static Logger logger = LoggerFactory.getLogger(QueryGenerator.class);

  public static String getSimpleQuery(QueryInterface params, Boolean highlightEnabled) {
    if (params.getFilter() != null) {
      return getFilteredQuery(params, highlightEnabled);
    }
    String queryString = params.getSearchText();
    if (queryString == null) {
      queryString = "*";
    }

    logger.debug("getSearchText {}", queryString);
    StringBuilder simpleQuery = new StringBuilder();
    if (queryString.contains("\"")) {
      queryString = StringEscapeUtils.escapeJson(queryString);
    }

    simpleQuery.append("{ \n"
        + "   \"query_string\": { \n"
        + "            \"query\": " + '"').append(queryString.toLowerCase()).append('"')
        .append("  \n}");

    if (highlightEnabled) {
      simpleQuery.append(",\n" +
          "  \"highlight\": { " +
          "\"pre_tags\" : [\"<strong>\"]," +
          "\"post_tags\" : [\"</strong>\"]," +
          "\"order\": \"score\", " +
          "\"require_field_match\": false, " +

          "\"encoder\" : \"html\"," +
          "    \"fields\": { " +
          "      \"*\": {} " +
          "    } " +
          "  }");
    }
    simpleQuery.append(" }");
    return simpleQuery.toString();
  }

  public static String getFilteredQuery(QueryInterface queryParams, Boolean highlightEnabled) {
    String queryString = queryParams.getSearchText();
    if (queryString == null) {
      queryString = "*";
    }

    logger.debug("getSearchText {}", queryString);
    StringBuilder simpleQuery = new StringBuilder();
    if (queryString.contains("\"")) {
      queryString = StringEscapeUtils.escapeJson(queryString);
    }
    if (queryString.equals("")) {
      queryString = "*";
    }
    String filter = getRelationshipFilter(queryParams);
    simpleQuery.append("{\n" +
//                " \"query\": {\n" +
        "    \"bool\": {\n" +
        "       \"must\": {\n" +
        "           \"match\":" + " {\n" +
        "               \"text\":\"" + queryString.toLowerCase() + "\"\n}" +
        "   " + (!filter.equals("") ? "}\n," + filter : "}\n") +
        "    }\n" +
//                "  }\n" +
        "}\n");

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

  private static String getRelationshipFilter(QueryInterface queryParams) {
    if (queryParams.getFilter() != null) {
      return "\"filter\":" + JsonUtils.toJson(queryParams.getFilter());
    }
    if (queryParams.getRelationships().isEmpty()) {
      return "";
    }

    // Open filter
    String filter = "\t\t \"filter\" : {\n" +
        "            \"and\" : [\n";
    boolean first = true;
    for (String relationship : queryParams.getRelationships()) {
      if (first) {
        filter += "     { \"exists\":{    \"field\" : \"tag." + relationship.toLowerCase() + ".*\" }}\n";
        first = false;
      } else {
        filter += "    ,{ \"exists\":{    \"field\" : \"tag." + relationship.toLowerCase() + ".*\" }}\n";
      }
    }

    filter += "      ]}\n"; // Close filter

    return filter;

  }
}
