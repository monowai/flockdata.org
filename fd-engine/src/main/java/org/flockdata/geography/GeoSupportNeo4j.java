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

import org.flockdata.registration.model.Tag;
import org.flockdata.track.model.GeoData;
import org.neo4j.graphdb.DynamicLabel;
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
public class GeoSupportNeo4j {
    @Autowired
    Neo4jTemplate template;

    private Logger logger = LoggerFactory.getLogger(GeoSupportNeo4j.class);

    @Cacheable(value = "geoData", key = "#loc.Id")
    public GeoData getGeoData(Tag loc) {
        logger.debug ( "Cache miss for {}", loc.getCode() );
        String query = "match (located:Tag)  , p= shortestPath((located:Tag)-[*1..3]->(c:Country)) where id(located)={locNode} return nodes(p)";
        HashMap<String, Object> params = new HashMap<>();
        params.put("locNode", loc.getId());
        Iterable<Map<String, Object>> queryResults = template.query(query, params);
        for (Map<String, Object> row : queryResults) {
            return getGeoData(row, loc);
        }
        return null;
    }

    GeoData getGeoData(Map<String, Object>row, Tag loc){
        Node country = null;
        Node state = null;
        Node city = null;
        if ( row.isEmpty())
            return null;

        Iterable<Object> nodes = (Iterable<Object>) row.get("nodes(p)");
        for (Object theNode : nodes) {
            Node node =(Node)theNode;
            if (isCountry(node))
                country = node;
            else if (isState(node))
                state = node;
            else if (isCity(node))
                city = node;
        }
        return getGeoData(loc, country, state, city);
    }

    boolean isCity(Node node){
        return node.hasLabel(DynamicLabel.label("City"));
    }

    boolean isCountry(Node node){
        return node.hasLabel(DynamicLabel.label("Country"));
    }

    boolean isState(Node node){
        return node.hasLabel(DynamicLabel.label("State"));
    }

    GeoData getGeoData(Tag loc, Node country, Node state, Node city){
        if (country == null && state == null && city == null)
            return null;

        String isoCode = null;
        String countryName = null;
        Double lat = null;
        Double lon = null;
        String stateName = null, stateCode = null;

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