/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.registration.service;


import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.repo.neo4j.dao.CompanyDao;
import com.auditbucket.registration.repo.neo4j.dao.FortressDao;
import com.auditbucket.registration.repo.neo4j.model.FortressNode;
import com.auditbucket.registration.repo.neo4j.model.FortressUserNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class FortressService {
    @Autowired
    private FortressDao fortressDao;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private SystemUserService sysUserService;

    @Autowired
    private CompanyDao companyDao;

    @Autowired
    private SecurityHelper securityHelper;

    public Fortress getFortress(Long id) {
        return fortressDao.findOne(id);
    }

    public FortressUser getUser(Long id) {
        return fortressDao.findOneUser(id);
    }

    public Fortress find(String fortressName) {
        Company ownedBy = getCompany();
        return companyDao.getFortress(ownedBy.getId(), fortressName);
    }

    private Company getCompany() {
        String userName = securityHelper.getUserName(true, false);
        SystemUser su = sysUserService.findByName(userName);
        if (su == null) {
            throw new SecurityException("Invalid user or password");
        }
        return su.getCompany();
    }

    public Fortress save(Fortress fortress) {
        return fortressDao.save(fortress);
    }

    /**
     * @param fortress     fortress to search
     * @param fortressUser user to locate
     * @return fortressUser identity, or creates it if it is not found
     */
    public FortressUser getFortressUser(Fortress fortress, String fortressUser) {
        return getFortressUser(fortress, fortressUser, true);
    }

    public FortressUser getFortressUser(Fortress fortress, String fortressUser, boolean createIfMissing) {
        if (fortressUser == null || fortress == null)
            throw new IllegalArgumentException("Don't go throwing null in here [" + (fortressUser == null ? "FortressUserNode]" : "FortressNode]"));

        FortressUser fu = fortressDao.getFortressUser(fortress.getId(), fortressUser);
        if (createIfMissing && fu == null)
            fu = addFortressUser(fortress.getId(), fortressUser);
        return fu;
    }

    public FortressUser save(FortressUser fortressUser) {
        return fortressDao.save(fortressUser);
    }

    public FortressUser addFortressUser(Long fortressId, String fortressUser) {
        Fortress fortress = getFortress(fortressId);
        if (fortress == null)
            throw new IllegalArgumentException("Unable to find requested fortress");


        Company company = fortress.getCompany();
        // this should never happen
        if (company == null)
            throw new IllegalArgumentException("[" + fortress.getName() + "] has no owner");

        registrationService.isAdminUser(company, "Unable to find requested fortress");

        FortressUser user = new FortressUserNode(fortress, fortressUser);
        return save(user);

    }

    public Fortress registerFortress(FortressInputBean fib) {
        Company company = getCompany();

        Fortress fortress = companyService.getFortress(company, fib.getName());
        if (fortress != null) {
            // Already associated, get out of here
            return fortress;
        }

        fortress = new FortressNode(fib, company);
        return save(fortress);

    }

    /**
     * Creates a fortress with the supplied name and will ignore any requests
     * to create Search Documents.
     *
     * @param fortressName company unique name
     * @return created fortress
     */
    public Fortress registerFortress(String fortressName) {
        FortressInputBean fb = new FortressInputBean(fortressName, true);
        return registerFortress(fb);
    }

    private Company getCompany(long fortressId) {
        return getFortress(fortressId).getCompany();

    }

    public List<Fortress> findFortresses(String companyName) {
        Company company = companyService.findByName(companyName);
        if (company == null)
            return null;      //ToDo: what kind of error page to return?
        if (companyService.getAdminUser(company, securityHelper.getUserName(true, true)) != null)
            return fortressDao.findFortresses(company.getId());
        else
            return null; //NotAuth
    }
}
