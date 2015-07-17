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

package org.flockdata.geography;

import org.flockdata.model.Tag;
import org.flockdata.model.GeoDataBean;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class GeoSupportNeo {
    @Autowired
    Neo4jTemplate template;

    private Logger logger = LoggerFactory.getLogger(GeoSupportNeo.class);

    @Cacheable(value = "geoData", key = "#loc.id")
    public GeoDataBean getGeoData(Tag loc) {
        logger.debug ( "Cache miss for {}", loc.getId() );
        //String query = "match (located:Tag)  , p= shortestPath((located:Tag)-[*1..3]->(c:Country)) where id(located)={locNode} return nodes(p)";
        String query = "match (located:Tag)-[r:state|address]->(o)-[*1..2]->(x:Country)  where id(located)={locNode} return located, o , x" ;
        HashMap<String, Object> params = new HashMap<>();
        params.put("locNode", loc.getId());
        Iterable<Map<String, Object>> queryResults = template.query(query, params);
        for (Map<String, Object> row : queryResults) {
            return getGeoData(row, loc);
        }
        return null;
    }

    private String getUserDefinedLabel(Node node ){
        Iterable<Label> labels = node.getLabels();
        for (Label label : labels) {
            String labelName = label.name();
            if ( !labelName.equals("Tag") && !labelName.equals("_Tag"))
                return labelName;
        }
        return null;

    }

    GeoDataBean getGeoData(Map<String, Object>row, Tag sourceTag){
        if ( row.isEmpty())
            return null;

        //Iterable<Node> nodes = (Iterable<Node>) row.get("nodes(p)");
        //;

        GeoDataBean geoData = new GeoDataBean();

        for (String key: row.keySet()) {
            Node node = (Node) row.get(key);
            String label = getUserDefinedLabel(node);
            // Check we don't add the same tag twice
            if ( label !=null && ! label.equals(sourceTag.getLabel())){

                String code;
                String name = null;
                Double lat = null;
                Double lon = null;
                code = (String) node.getProperty("code");
                if (node.hasProperty("name")) {
                    name = (String) node.getProperty("name");
                    if ( name.equals(code))
                        name = null;
                }
                if (node.hasProperty("props-latitude"))
                    lat = (Double) node.getProperty("props-latitude");

                if (node.hasProperty("props-longitude"))
                    lon = (Double) node.getProperty("props-longitude");
                geoData.add(label.toLowerCase(), code, name, lat, lon);
            }
        }

        return geoData;
    }
}