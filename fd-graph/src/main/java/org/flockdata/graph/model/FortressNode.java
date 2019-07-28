package org.flockdata.graph.model;

import lombok.Builder;
import lombok.Data;
import org.flockdata.data.Company;
import org.flockdata.data.Fortress;
import org.flockdata.data.Segment;

/**
 * @author mikeh
 * @since 15/06/18
 */
@Builder
@Data
public class FortressNode implements Fortress {
  private Long id;
  private String code;
  private String name;
  private Company company;
  private Boolean searchEnabled;
  private Boolean storeEnabled;
  private Boolean system;
  private String rootIndex;
  private Segment defaultSegment;
  private String timeZone;
  private Boolean enabled;

  @Override
  public Boolean isSearchEnabled() {
    return searchEnabled;
  }

  @Override
  public Boolean isStoreEnabled() {
    return storeEnabled;
  }

  @Override
  public Boolean isSystem() {
    return system;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }
}
