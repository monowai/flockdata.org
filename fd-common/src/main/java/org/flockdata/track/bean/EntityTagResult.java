package org.flockdata.track.bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.model.EntityTag;
import org.flockdata.registration.TagResultBean;

import java.util.Map;

/**
 * Created by mike on 19/02/16.
 */
public class EntityTagResult {


    private  GeoDataBeans geoData;
    private Map<String,Object> props;
    private TagResultBean tag;
    private String relationship    ;

    EntityTagResult(){}

    public EntityTagResult(EntityTag logTag) {
        this();
        logTag.getTag();
        props = logTag.getProperties();
        this.tag = new TagResultBean(logTag.getTag());
        this.relationship = logTag.getRelationship();
        this.geoData = logTag.getGeoData();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public GeoDataBeans getGeoData() {
        return geoData;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Map<String, Object> getProps() {
        return props;
    }

    public TagResultBean getTag() {
        return tag;
    }

    public String getRelationship() {
        return relationship;
    }
}
