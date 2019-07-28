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

package org.flockdata.geography.dao;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.math.NumberUtils;
import org.flockdata.data.Tag;
import org.flockdata.engine.data.graph.TagNode;
import org.flockdata.track.bean.GeoDataBean;
import org.flockdata.track.bean.GeoDataBeans;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Service;

/**
 * @tag Geo, Neo4j
 */
@Service
public class GeoSupportNeo {
  private final
  Neo4jTemplate template;

  private Logger logger = LoggerFactory.getLogger(GeoSupportNeo.class);

  @Autowired
  public GeoSupportNeo(Neo4jTemplate template) {
    this.template = template;
  }

  @Cacheable(value = "geoData", key = "#loc.id")
  public GeoDataBeans getGeoData(String query, Tag loc) {
    //logger.info("Cache miss f {}, Tag {}", e.getId(), loc.getId());
    HashMap<String, Object> params = new HashMap<>();
    params.put("locNode", loc.getId());

//        String query = getQuery(e);
    Map<String, Object> rows = template.query(query, params).singleOrNull();

    if (rows == null || rows.isEmpty()) {
      return null;
    }

    return getGeoData(rows, loc);
  }

  GeoDataBeans getGeoData(Map<String, Object> row, Tag sourceTag) {
    if (row.isEmpty()) {
      return null;
    }

    GeoDataBeans geoBeans = new GeoDataBeans();

    if (row.containsKey("nodes")) {
      Iterable<Node> nodes = (Iterable<Node>) row.get("nodes");
      for (Node node : nodes) {
        setFromNode(sourceTag, geoBeans, node);

      }
    } else {
      for (String key : row.keySet()) {
        Node node = (Node) row.get(key);
        setFromNode(sourceTag, geoBeans, node);
      }
    }

    return geoBeans;
  }

  private GeoDataBean setFromNode(Tag sourceTag, GeoDataBeans geoBeans, Node node) {
    GeoDataBean geoData = new GeoDataBean();
    String label = getUserDefinedLabel(node);
    // Check we don't add the same tag twice
    if (label != null) {

      String code;
      String name = null;
      Double lat = null;
      Double lon = null;
      code = (String) node.getProperty("code");
      if (node.hasProperty("name")) {
        name = (String) node.getProperty("name");
        if (name.equals(code)) {
          name = null;
        }
      }
      if (node.hasProperty(TagNode.NODE_LAT)) {
        String val = node.getProperty(TagNode.NODE_LAT).toString();
        if (NumberUtils.isNumber(val)) {
          lat = Double.parseDouble(val);
        }
      }

      if (node.hasProperty(TagNode.NODE_LON)) {
        String val = node.getProperty(TagNode.NODE_LON).toString();
        if (NumberUtils.isNumber(val)) {
          lon = Double.parseDouble(val);
        }
      }

      geoData.add(label.toLowerCase(), code, name, lat, lon);
      geoBeans.add(label.toLowerCase(), geoData);

      if (label.equals(sourceTag.getLabel())) {
        geoData.setCode(null);
        geoData.setName(null);
      }

    }

    return geoData;
  }

  private String getUserDefinedLabel(Node node) {
    Iterable<Label> labels = node.getLabels();
    for (Label label : labels) {
      String labelName = label.name();
      if (!labelName.equals("Tag") && !labelName.equals("_Tag")) {
        return labelName;
      }
    }
    return null;

  }


}