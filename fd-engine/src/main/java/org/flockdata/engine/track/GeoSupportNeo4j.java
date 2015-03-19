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

package org.flockdata.engine.track;

import org.flockdata.track.model.GeoData;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Service;

@Service
public class GeoSupportNeo4j {
    @Autowired
    Neo4jTemplate template;

    private TraversalDescription geoTraversal =null ;

    TraversalDescription getGeoTraverser () {
        if (geoTraversal==null )


            geoTraversal = template.getGraphDatabaseService().traversalDescription()
                    .depthFirst()
                    .uniqueness(Uniqueness.NODE_PATH);

        return geoTraversal;
    }


    @Cacheable(value = "geoData", key = "#loc.Id")
    public GeoData getGeoData(Node loc) {
        return getGeoData(
                getGeoTraverser()
                        .evaluator(Evaluators.fromDepth(1))
                        .evaluator(Evaluators.toDepth(2))  // Keep an eye on this - may need to be 3
                        .evaluator(Evaluators.excludeStartPosition())
                        .evaluator(new GeoEvaluator())
                        .traverse(loc)
                        .nodes()
                , loc);

        //String query = "match (located:_Tag)-[*0..2]-(country:Country) where id(located)={locNode} optional match located-[*0..2]->(state:State) return country, state";
//            String query = "match (located:_Tag)  , p= shortestPath((located:_Tag)-[*1..2]-(c:Country)) where id(located)={locNode} return nodes(p)";
//        HashMap<String, Object> params = new HashMap<>();
//        params.put("locNode", loc.getId());
//        Result<Map<String, Object>> queryResults = template.query(query, params);
//        for (Map<String, Object> row : queryResults) {
//            return getGeoData(row, loc);
//        }
    }

    GeoData getGeoData(ResourceIterable<Node> row, Node loc) {

        String isoCode = null;
        String countryName = null;
        Double lat = null;
        Double lon = null;
        String stateName = null, stateCode = null;

        Node country = null;
        Node state = null;
        Node city = null;

        for (Node node : row) {
            if (node.hasLabel(DynamicLabel.label("Country")))
                country = node;
            else if (node.hasLabel(DynamicLabel.label("State")))
                state = node;
            else if (node.hasLabel(DynamicLabel.label("City")))
                city = node;
        }
        if (country == null && state == null && city == null)
            return null;

        String cityName;
        if (city != null && city.hasProperty("name"))
            cityName = city.getProperty("name").toString();
        else
            cityName = (String) loc.getProperty("name");

        if (country != null && country.hasProperty("code")) {
            // ToDo: Need a Country object
            isoCode = (String) country.getProperty("code");
            if (country.hasProperty("name"))
                countryName = (String) country.getProperty("name");
            Object latitude = null;
            Object longitude = null;

            if (country.hasProperty("props-latitude"))
                latitude = country.getProperty("props-latitude");

            if (country.hasProperty("props-longitude"))
                longitude = country.getProperty("props-longitude");

            if ((latitude != null && longitude != null) && !(latitude.equals("") || longitude.equals(""))) {
                lat = Double.parseDouble(latitude.toString());
                lon = Double.parseDouble(longitude.toString());
            }
        }
        if (state != null && state.hasProperty("name"))
            stateName = (String) state.getProperty("name");
        if (state != null && state.hasProperty("code"))
            stateCode = (String) state.getProperty("code");
        if (country == null)
            return null;
        GeoData geoData = new GeoData(isoCode, countryName, cityName, stateName);
        geoData.setLatLong("country", lat, lon);
        geoData.setStateCode(stateCode);
        return geoData;
    }
}