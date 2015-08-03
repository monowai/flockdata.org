package org.flockdata.transform;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Object has support for nested properties
 *
 * Created by mike on 30/07/15.
 */
public interface UserProperties {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Map<String, Object> getProperties();

    void setProperty(String key, Object value);

    Object getProperty(String key);
}
