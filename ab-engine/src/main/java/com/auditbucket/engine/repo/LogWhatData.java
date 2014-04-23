/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.engine.repo;

import com.auditbucket.helper.CompressionHelper;
import com.auditbucket.track.model.LogWhat;
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
public class LogWhatData implements LogWhat {

    @JsonIgnore
    private
    byte[] whatBytes;

    private Boolean compressed;

    @Transient
    private Map<String, Object> what;

    @Transient
    private
    ObjectMapper objectMapper = new ObjectMapper();

    protected LogWhatData() {
    }

    public LogWhatData(byte[] whatInformation, boolean compressed) {
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
                what = objectMapper.readValue(getWhat(), Map.class);
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
