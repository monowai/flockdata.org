package com.auditbucket.engine.repo.neo4j.model;

import com.auditbucket.audit.model.AuditWhat;
import com.auditbucket.helper.CompressionHelper;
import com.auditbucket.helper.CompressionResult;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Mike Holdsworth
 * Since: 4/09/13
 */
@NodeEntity
public class AuditWhatNode implements AuditWhat {
    @GraphId
    private Long id;

    @JsonIgnore
    byte[] whatBytes;

    Boolean compressed;

    @Transient
    private Map<String, Object> what;

    @Transient
    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @JsonIgnore
    public String getId() {
        if (id == null)
            return "";
        return id.toString();
    }

    @Override
    @JsonIgnore
    public String getWhat() {
        return CompressionHelper.decompress(whatBytes, compressed);
    }


    public Map<String, Object> getWhatMap() {

        if (what != null)
            return what;
        try {
            if (whatBytes != null) {
                what = objectMapper.readValue(CompressionHelper.decompress(whatBytes, compressed), Map.class);
                return what;
            }

        } catch (IOException e) {
        }
        what = new HashMap<>();
        what.put("empty", "{}");
        return what;
    }

    @Override
    public boolean isCompressed() {
        return compressed;
    }

    public void setJsonWhat(String what) {
        CompressionResult result = CompressionHelper.compress(what);

        this.whatBytes = result.getAsBytes();
        this.compressed = result.getMethod() == CompressionResult.Method.GZIP;
    }

}
