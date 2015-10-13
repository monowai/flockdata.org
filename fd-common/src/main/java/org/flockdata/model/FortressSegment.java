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
        this.key = fortress.getCode() +"/"+code.toLowerCase();
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
}
