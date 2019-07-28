package org.flockdata.graph.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.flockdata.data.Company;
import org.flockdata.data.SystemUser;
import org.flockdata.registration.RegistrationBean;
import org.neo4j.driver.v1.types.Node;

/**
 * @author mikeh
 * @since 16/06/18
 */
@Builder
@Data
@AllArgsConstructor
public class SystemUserNode implements SystemUser {
  private Long id;

  private String name;

  //@Indexed
  private String login;

  private String email;

  // @Indexed
  private String apiKey;

  private Company company;

  @Builder.Default
  private boolean active = true;

  public SystemUserNode() {
    active = true;
  }

  public static SystemUser build(Company company, Node node) {
    return SystemUserNode.builder()
        .company(company)
        .id(node.id())
        .login(node.get("login").asString())
        .name(node.get("name").asString())
        .active(node.get("active").asBoolean())
        .apiKey(node.get("apiKey").asString())
        .email(node.get("email").asString())
        .build();
  }

  public static SystemUser build(Company company, RegistrationBean regBean, String apiKey) {
    return SystemUserNode.builder()
        .login(regBean.getLogin())
        .name(regBean.getName())
        .login(regBean.getLogin())
        .apiKey(apiKey)
        .company(company)
        .build();
  }

}
