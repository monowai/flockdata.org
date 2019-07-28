package org.flockdata.graph.model;

import lombok.Builder;
import lombok.Data;
import org.flockdata.data.Company;
import org.neo4j.driver.v1.types.Node;

/**
 * @author mikeh
 * @since 15/06/18
 */
@Data
@Builder
public class CompanyNode implements Company {
  private Long id;
  private String name;
  private String code;
  private String apiKey;

  public static Company build(Node node) {
    return CompanyNode.builder()
        .id(node.id())
        .name(node.get("name").asString())
        .code(node.get("code").asString())
        .apiKey(node.get("apiKey").asString())
        .build();
  }
}
