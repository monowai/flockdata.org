package org.flockdata.graph.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import lombok.Builder;
import lombok.Data;
import org.flockdata.data.Entity;
import org.flockdata.data.Fortress;
import org.flockdata.data.FortressUser;
import org.flockdata.data.Segment;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.neo4j.driver.v1.types.Node;

/**
 * @author mikeh
 * @since 10/06/18
 */
@Data
@Builder
public class EntityNode implements Entity {
  // transient
  boolean newEntity;
  private Long id;
  private String name;
  private String code;
  private String key;
  private String type;
  private String extKey;
  @Builder.Default
  private List<String> labels = Arrays.asList("Entity");
  private Integer search;
  @Builder.Default
  private long dateCreated = new DateTime().toDateTime(DateTimeZone.UTC).toDate().getTime();
  private Long lastUpdate;
  private Long fortressCreate;
  private Long fortressLastWhen;
  private String searchKey;
  private FortressUser lastUser;
  @JsonIgnore
  private Fortress fortress;
  private Segment segment;

  public static EntityNode build(Node node) {
    return EntityNode.builder()
        .id(node.id())
        .key(node.get("key").asString())
        .name(node.get("name").asString())
        .code(node.get("code").asString())
        .build();
  }

  public void bumpUpdate() {
    if (id != null) {
      lastUpdate = new DateTime().toDateTime(DateTimeZone.UTC).toDateTime().getMillis();
    }
  }

  @Override
  public String getSearchKey() {
    return (searchKey == null ? code : searchKey);

  }

  public void setSearchKey(String searchKey) {
    // By default the searchkey is the code. Let's save disk space
    if (searchKey != null && searchKey.equals(code)) {
      this.searchKey = null;
    } else {
      this.searchKey = searchKey;
    }
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
  public boolean isNewEntity() {
    return false;
  }

  @Override
  public boolean isNoLogs() {
    return false;
  }

  @Override
  public String getEvent() {
    return null;
  }

  @JsonIgnore
  public DateTime getFortressUpdatedTz() {
    if (fortressLastWhen == null) {
      return null;
    }
    return new DateTime(fortressLastWhen, DateTimeZone.forTimeZone(TimeZone.getTimeZone(segment.getFortress().getTimeZone())));
  }

  @Override
  @JsonIgnore
  public DateTime getFortressCreatedTz() {
    return new DateTime(fortressCreate, DateTimeZone.forTimeZone(TimeZone.getTimeZone(segment.getFortress().getTimeZone())));
  }

  @Override
  public Long getLastUpdate() {
    return lastUpdate == null ? dateCreated : lastUpdate;
  }


}
