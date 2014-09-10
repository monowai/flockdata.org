package com.auditbucket.search.helper;

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

        simpleQuery.append("{" + "  query: {"
                + "    bool: { "
                + "      should: ["
                + "        {query_string: { "
                + "            query: " + '"').append(queryString).append('"')
                .append("          }")
                .append("        }")
                .append("      ]")
                .append("    }")
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
