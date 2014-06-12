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
import com.auditbucket.registration.model.Tag;

import java.util.ArrayList;

/**
 * User: mike
 * Date: 12/06/14
 * Time: 11:37 AM
 */
public class NeoSyntaxHelper {

    public static String getLabels(ArrayList<String> input) {
        if (input == null || input.isEmpty())
            return ":_MetaHeader";
        // create a neo4j label index
        return getNeoString(":", input, " ");
    }

    public static String getConcepts(ArrayList<String> inputBean) {
        if ( inputBean==null || inputBean.isEmpty())
            return Tag.DEFAULT;
        return getNeoString(":", inputBean, " ");
    }

    public static String getRelationships(ArrayList<String> inputBean) {
        if ( inputBean==null || inputBean.isEmpty())
            return "";
        return getNeoString(":", inputBean, " |");
    }

    private static String getNeoString(String delimiter, ArrayList<String> input, String join) {
        String result = (delimiter.equals(":")? ":": "");
        for (String s : input) {
            if ( s != null ) {
                if ( s.contains(" "))
                    s = "`"+s+"`" ;

                if ( result.equals(delimiter) || result.equals(""))
                    result = result + s+"";
                else
                    result = result +join+delimiter +s ;
            }
        }
        if ( result.equals(delimiter))
            result = "";
        return result;
    }

}
