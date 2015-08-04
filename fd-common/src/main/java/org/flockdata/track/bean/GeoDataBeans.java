package org.flockdata.track.bean;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains index data of geoDataBeans and points
 *
 * Collection of geo data
 * Collection of points
 * A description for the collection of GeoDataBean indexed objects
 *
 * Created by mike on 4/08/15.
 */
public class GeoDataBeans {
    Map<String,GeoDataBean> geoBeans = null;
    Map<String, String> points = null;

    String description = null;

    public void add(String prefix, GeoDataBean geoBean) {
        if ( geoBeans == null )
            geoBeans = new HashMap<>();


        geoBeans.put(prefix, geoBean);
        if ( geoBean.getPoints() !=null ){
            if ( points == null )
                points = new HashMap<>();
            points.put(prefix, geoBean.getPoints().get(prefix));
        }

        if ( geoBean.getName() !=null ){
            if ( description == null )
                description = (prefix.equalsIgnoreCase("state") ? geoBean.getCode():geoBean.getName());
            else
                description = description + ", "+  (prefix.equalsIgnoreCase("state") ? geoBean.getCode():geoBean.getName());
        }

    }

    public Map<String,GeoDataBean> getGeoBeans() {
        return geoBeans;
    }

    // GeoJson map points
    public Map<String, String> getPoints() {
        return points;
    }

    public String getDescription() {
        if ( geoBeans == null || geoBeans.size() == 1)
            return null; // Value was not concatenated so can be found elsewhere
        return description;
    }

}
