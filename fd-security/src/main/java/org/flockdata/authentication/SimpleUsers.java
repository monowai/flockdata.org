/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.authentication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configures user/password/roles from an external configuration source.
 * <p>
 * <p>
 * YAML example
 * ============
 * org.fd.auth:
 * simple:
 * users:
 * mike:
 * pass: 123
 * roles: FD_USER;FD_ADMIN
 *
 * @author mholdsworth
 * @since 11/05/2016
 */
@ConfigurationProperties(prefix = "org.fd.auth.simple")
@Configuration
public class SimpleUsers {

  private HashMap<String, UserEntry> users;

  public HashMap<String, UserEntry> getUsers() {
    return users;
  }

  public void setUsers(HashMap<String, UserEntry> users) {
    this.users = users;
  }

  void createDefault() {
    users = new HashMap<>(1);
    users.put("demo", new UserEntry()
        .setPass("123")
        .setRoles(FdRoles.FD_ADMIN + ";" + FdRoles.FD_USER));
  }

  @SuppressWarnings("WeakerAccess")
  public static class UserEntry {
    private String pass;

    private Collection<String> roles = new ArrayList<>();

    public String getPass() {
      return pass;
    }

    public UserEntry setPass(String pass) {
      this.pass = pass;
      return this;
    }

    public Collection<String> getRoles() {
      return roles;
    }

    public UserEntry setRoles(String roles) {
      String[] splitRoles = roles.split(";");
      for (String splitRole : splitRoles) {
        this.roles.add(splitRole.trim());
      }
      this.roles.add("USER"); // ToDo: Role required??
      return this;
    }
  }
}
