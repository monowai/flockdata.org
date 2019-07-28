/*
 *  Copyright 2012-2017 the original author or authors.
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

package org.flockdata.registration;


import lombok.Builder;
import lombok.Data;
import org.flockdata.data.Company;
import org.flockdata.track.bean.CompanyInputBean;

/**
 * @author mholdsworth
 * @since 14/05/2013
 */
@Builder
@Data
public class RegistrationBean {
  private String name;
  private String login;
  private String companyName;
  private Company company;
  private boolean unique;
  private String email;

  public RegistrationBean setCompany(final Company company) {
    this.company = new CompanyInputBean(company.getName());
    this.companyName = company.getName();
    return this;
  }

  @Override
  public String toString() {
    return "RegistrationBean{" +
        "companyName='" + companyName + '\'' +
        ", company=" + company +
        ", login='" + login + '\'' +
        ", name='" + name + '\'' +
        '}';
  }

}


