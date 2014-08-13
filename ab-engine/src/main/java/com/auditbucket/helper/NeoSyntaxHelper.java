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

import java.util.Collection;

/**
 * User: mike
 * Date: 12/06/14
 * Time: 11:37 AM
 */
public class NeoSyntaxHelper {

    public static String getLabels(String columnName, Collection<String> values) {
        if (values == null || values.isEmpty())
            return ":_MetaHeader";
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

}
