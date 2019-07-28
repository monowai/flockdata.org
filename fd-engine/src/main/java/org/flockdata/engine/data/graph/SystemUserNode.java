/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

package org.flockdata.engine.data.graph;

import org.flockdata.data.Company;
import org.flockdata.data.SystemUser;
import org.flockdata.registration.RegistrationBean;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

/**
 * Authorised user acting on behalf of fortress
 *
 * @tag SystemUser, Node
 */
@NodeEntity
@TypeAlias(value = "SystemUser")
public class SystemUserNode implements SystemUser {
  @GraphId
  private Long id;

  private String name = null;

  //@Indexed
  @Indexed(unique = true)
  private String login;

  //    @Indexed(unique = true)
  private String email;

  @Indexed
  private String apiKey;

  //@Relationship( type = "ACCESSES", direction = Relationship.OUTGOING)
  @Fetch
  @RelatedTo(type = "ACCESSES", direction = Direction.OUTGOING)
  private CompanyNode company;
  private boolean active = true;

  protected SystemUserNode() {
  }

  public SystemUserNode(String name, String login, Company company, boolean admin) {
    setName(name);
    if (login == null) {
      login = name;
    }
    setLogin(login);

    if (admin && company != null) {
      setCompanyAccess(company);
    }
  }

  public SystemUserNode(RegistrationBean regBean, Company company) {
    this.login = regBean.getLogin();
    this.name = regBean.getName();
    this.email = regBean.getEmail();
    if (company != null) {
      this.setCompanyAccess(company);
    }
  }

  @Override
  public String getName() {
    return name;
  }

  SystemUserNode setName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public String getLogin() {
    return login;
  }

  public SystemUserNode setLogin(String login) {
    this.login = login.toLowerCase();
    return this;

  }

  @Override
  public String getApiKey() {
    return apiKey;
  }

  public SystemUserNode setApiKey(String openUID) {
    this.apiKey = openUID;
    return this;
  }

  @Override
  public Company getCompany() {
    return company;
  }

  @Override
  public String getEmail() {
    return email;
  }

  @Override
  public Long getId() {
    return id;

  }

  public void setCompanyAccess(Company company) {
    this.company = CompanyNode.builder()
        .code(company.getCode())
        .name(company.getName())
        .apiKey(company.getApiKey())
        .build();
  }

  @Override
  public String toString() {
    return "SystemUser{" +
        "id=" + id +
        ", login='" + login + '\'' +
        ", company=" + company +
        '}';
  }

  @Override
  public boolean isActive() {
    return active;
  }
}
