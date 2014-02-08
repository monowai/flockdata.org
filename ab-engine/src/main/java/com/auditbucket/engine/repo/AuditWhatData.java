package com.auditbucket.engine.repo;

import com.auditbucket.audit.model.AuditWhat;
import com.auditbucket.helper.CompressionHelper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.annotation.Transient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Mike Holdsworth
 * POJO for transmitting what data from a KV store
 * Since: 4/09/13
 */
public class AuditWhatData implements AuditWhat {

    @JsonIgnore
    private
    byte[] whatBytes;

    private Boolean compressed;

    @Transient
    private Map<String, Object> what;

    @Transient
    private
    ObjectMapper objectMapper = new ObjectMapper();

    protected AuditWhatData() {
    }

    public AuditWhatData(byte[] whatInformation, boolean compressed) {
        this();
        this.setWhatBytes(whatInformation);
        this.compressed = compressed;
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

    void setWhatBytes(byte[] whatBytes) {
        this.whatBytes = whatBytes;
    }

}
