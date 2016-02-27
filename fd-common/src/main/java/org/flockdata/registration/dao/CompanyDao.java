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

package org.flockdata.registration.dao;


import org.flockdata.model.Company;
import org.flockdata.model.SystemUser;

import java.util.Collection;

/**
 * Company represents a unique organisation who interacts with the system
 * API To abstract interactions with underlying implementations
 *
 * User: Mike Holdsworth
 * Date: 20/04/13
 * Time: 6:31 PM
 */
public interface CompanyDao {
    Company update(Company systemUser);

    Company findByPropertyValue(String property, Object value);

    SystemUser getAdminUser(Long companyId, String name);

    Company create(String companyName, String uniqueKey);

    Collection<Company> findCompanies(Long sysUserId);

    Collection<Company> findCompanies(String userApiKey);

    Company create(Company company);
}
