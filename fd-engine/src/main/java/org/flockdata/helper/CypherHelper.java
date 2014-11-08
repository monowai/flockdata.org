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

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import java.util.Collection;

/**
 * User: mike
 * Date: 12/06/14
 * Time: 11:37 AM
 */
public class CypherHelper {

    public static String getLabels(String columnName, Collection<String> values) {
        if (values == null || values.isEmpty())
            return ":_Entity";
        // create a neo4j label index
        // DAT-109
        return getNeoString(columnName, ":", values, " or ");
    }

    public static String getConcepts(String columnName, Collection<String> values) {
        if (values == null || values.isEmpty())
            return "";
        // Based on neo4j label index, but no default. Filters on Tags
        // DAT-109
        return getNeoString(columnName, ":", values, " or ");

    }

    public static String getRelationships(Collection<String> values) {
        if ( values ==null || values.isEmpty())
            return "";
        return ":"+getNeoString(null, ":", values, " |");
    }

    private static String getNeoString(String columnName, String delimiter, Collection<String> input, String join) {
        String result = "";//(delimiter.equals(":")? ":": "");
        for (String field : input) {
            if ( field != null ) {
                // ToDo: Fix this hack
                if ( field.equals("User"))
                    field = "_FortressUser";
                if ( field.contains(" ") || field.contains("-")|| field.contains(".") || field.matches("^[\\d\\-\\.]+$"))
                    field = "`"+ field +"`" ;

                if ( result.equals(delimiter) || result.equals(""))
                    result = result + (columnName!=null?columnName+delimiter:"")+ field +"";
                else
                    result = result +join+(columnName!=null?columnName:"")+delimiter + field;
            }
        }
        if ( result.equals(delimiter))
            result = "";
        return result;
    }

    public static boolean isEntity (Node node ){
        // DAT-279
        for (Label label : node.getLabels()) {
            if ( label.name().equals("_Entity"))
                return true;
        }
        return false;
    }

}
