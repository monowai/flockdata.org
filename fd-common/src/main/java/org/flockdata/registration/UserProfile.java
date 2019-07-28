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

package org.flockdata.registration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserProfile implements Serializable {

  /**
   *
   */
  private static final long serialVersionUID = 8908570723211178151L;

  private String userId;
  private String userName;
  private String userEmail;
  private String status;
  private String company;
  private List<String> userRoles = new ArrayList<>();
  private String apiKey;

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getUserEmail() {
    return userEmail;
  }

  public void setUserEmail(String userEmail) {
    this.userEmail = userEmail;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void addUserRole(String role) {
    userRoles.add(role);
  }

  public Object[] getUserRoles() {
    return this.userRoles.toArray();
  }

  @Override
  public String toString() {
    return "UserProfile [userId=" + userId + ", userName=" + userName
        + ", userEmail=" + userEmail + ", status=" + status
        + ", userRoles=" + userRoles + ", apiKey=" + apiKey + "]";
  }

  public String getCompany() {
    return company;
  }

  public void setCompany(String company) {
    this.company = company;
  }


}
