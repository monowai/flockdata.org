/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

package org.flockdata.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.*;

/**
 * Created by mike on 13/10/15.
 */

@NodeEntity
@TypeAlias("FortressSegment")
public class FortressSegment {
    @GraphId
    Long id;

    @Indexed
    private String code;

    @Indexed(unique = true)
    private String key;

    public static final String DEFAULT = "Default";

    @RelatedTo(type = "DEFINES", direction = Direction.INCOMING)
    @Fetch
    Fortress fortress;

    FortressSegment () {}

    public FortressSegment (Fortress fortress) {
        this(fortress, DEFAULT);
        this.fortress = fortress;
    }
    public FortressSegment (Fortress fortress, String code) {
        this();
        this.fortress = fortress;
        this.code = code;
        if ( fortress == null)
            throw new IllegalArgumentException("An invalid fortress was passed in");
        this.key = key(fortress.getCode(), code);
    }

    public static String key(String fortressCode, String segmentCode ){
        if ( segmentCode == null )
            return null;
        return fortressCode +"-"+segmentCode.toLowerCase();
    }

    public String getCode() {
        return code;
    }

    public Fortress getFortress() {
        return fortress;
    }

    public Long getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    @JsonIgnore
    public boolean isDefault() {
        return code.equals(DEFAULT);
    }

    @JsonIgnore
    public Company getCompany() {
        return fortress.getCompany();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FortressSegment)) return false;

        FortressSegment segment = (FortressSegment) o;

        if (id != null ? !id.equals(segment.id) : segment.id != null) return false;
        if (code != null ? !code.equals(segment.code) : segment.code != null) return false;
        if (key != null ? !key.equals(segment.key) : segment.key != null) return false;
        return !(fortress != null ? !fortress.getId().equals(segment.fortress.getId()) : segment.fortress.getId() != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (fortress != null ? fortress.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FortressSegment{" +
                "code='" + code + '\'' +
                "key='" + key + '\'' +
                '}';
    }
}
