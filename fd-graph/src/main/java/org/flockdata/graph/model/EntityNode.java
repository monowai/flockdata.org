package org.flockdata.graph.model;

import lombok.Builder;
import lombok.Data;
import org.flockdata.data.Entity;
import org.flockdata.data.Fortress;
import org.flockdata.data.FortressUser;
import org.flockdata.data.Segment;
import org.neo4j.driver.v1.types.Node;

import java.util.Map;

/**
 * @author mikeh
 * @since 10/06/18
 */
@Data
@Builder
public class EntityNode implements Entity {
    private Long id;
    private String name;
    private String code;
    private String key;
    private String type;
    private Integer search;
    private FortressUser lastUser;

    public static EntityNode build(Node node) {
        return EntityNode.builder()
            .id(node.id())
            .key(node.get("key").asString())
            .name(node.get("name").asString())
            .code(node.get("code").asString())
            .build();
    }

    @Override
    public Long getLastUpdate() {
        return null;
    }

    @Override
    public FortressUser getCreatedBy() {
        return null;
    }

    @Override
    public Map<String, Object> getProperties() {
        return null;
    }

    @Override
    public boolean isSearchSuppressed() {
        return false;
    }

    @Override
    public String getSearchKey() {
        return null;
    }

    @Override
    public long getDateCreated() {
        return 0;
    }

    @Override
    public org.joda.time.DateTime getFortressCreatedTz() {
        return null;
    }

    @Override
    public boolean isNewEntity() {
        return false;
    }

    @Override
    public boolean isNoLogs() {
        return false;
    }

    @Override
    public Fortress getFortress() {
        return null;
    }

    @Override
    public Segment getSegment() {
        return null;
    }

    @Override
    public String getEvent() {
        return null;
    }


    @Override
    public org.joda.time.DateTime getFortressUpdatedTz() {
        return null;
    }


}
