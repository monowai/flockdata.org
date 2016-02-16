/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

package org.flockdata.neo4j.helper;

import org.flockdata.authentication.registration.bean.TagResultBean;

import java.util.Map;

/**
 * Helpers for working with cypher
 *
 * Created by mike on 6/07/15.
 */
public class CypherUtils {

    /**
     * Builds a cypher clause with user defined properties
     *
     * @param query the query to start appending to
     * @param props source map containing properties
     * @param args  a map to hold arguments used to populate query
     * @return
     */
    public static String buildAndSetProperties(String query, Map<String,Object>props, Map<String,Object> args) {

        for (String udfKey : props.keySet()) {
            args.put( "p"+udfKey, props.get(udfKey));
            query = query + ", `"+ TagResultBean.PROPS_PREFIX+"" + udfKey +"`: {p" +udfKey +"}";
        }

        return query;

    }
}
