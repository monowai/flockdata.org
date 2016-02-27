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

package org.flockdata.registration.service;

import org.flockdata.model.Company;
import org.flockdata.model.SystemUser;

import java.util.Collection;

/**
 * User: mike
 * Date: 22/08/14
 * Time: 10:17 AM
 */
public interface CompanyService {
    Company findByName(String companyName);

    Company findByCode(String code);

    SystemUser getAdminUser(Company company, String name);

    Company create(String companyName);

//    @Cacheable(value = "companyKeys", unless = "#result == null")
    Company findByApiKey(String apiKey);

    Collection<Company> findCompanies(String userApiKey);

    Collection<Company> findCompanies();
}
