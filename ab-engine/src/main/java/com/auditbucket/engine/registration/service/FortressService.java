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

package com.auditbucket.engine.registration.service;


import com.auditbucket.engine.registration.bean.FortressInputBean;
import com.auditbucket.engine.registration.dao.CompanyDaoI;
import com.auditbucket.engine.registration.dao.FortressDaoI;
import com.auditbucket.engine.registration.repo.neo4j.model.Fortress;
import com.auditbucket.engine.registration.repo.neo4j.model.FortressUser;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.IFortressUser;
import com.auditbucket.registration.model.ISystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FortressService {
    @Autowired
    private FortressDaoI fortressDao;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private SystemUserService sysUserService;

    @Autowired
    private CompanyDaoI companyDao;

    @Autowired
    private SecurityHelper securityHelper;

    public IFortress getFortress(Long id) {
        return fortressDao.findOne(id);
    }

    public IFortressUser getUser(Long id) {
        return fortressDao.findOneUser(id);
    }

    public IFortress find(String fortressName) {
        ICompany ownedBy = getCompany();
        return companyDao.getFortress(ownedBy.getId(), fortressName);
    }

    private ICompany getCompany() {
        String userName = securityHelper.getUserName(true, false);
        ISystemUser su = sysUserService.findByName(userName);
        if (su == null) {
            throw new SecurityException("Invalid user or password");
        }
        return su.getCompany();
    }

    @Transactional
    public IFortress save(IFortress fortress) {
        return fortressDao.save(fortress);
    }

    /**
     * @param fortress     fortress to search
     * @param fortressUser user to locate
     * @return fortressUser identity, or creates it if it is not found
     */
    public IFortressUser getFortressUser(IFortress fortress, String fortressUser) {
        return getFortressUser(fortress, fortressUser, true);
    }

    public IFortressUser getFortressUser(IFortress fortress, String fortressUser, boolean createIfMissing) {
        if (fortressUser == null || fortress == null)
            throw new IllegalArgumentException("Don't go throwing null in here [" + (fortressUser == null ? "FortressUser]" : "Fortress]"));

        IFortressUser fu = fortressDao.getFortressUser(fortress.getId(), fortressUser);
        if (createIfMissing && fu == null)
            fu = addFortressUser(fortress.getId(), fortressUser);
        return fu;
    }

    @Transactional
    public IFortressUser save(IFortressUser fortressUser) {
        return fortressDao.save(fortressUser);
    }

    @Transactional
    public IFortressUser addFortressUser(Long fortressId, String fortressUser) {
        IFortress fortress = getFortress(fortressId);
        if (fortress == null)
            throw new IllegalArgumentException("Unable to find requested fortress");


        ICompany company = fortress.getCompany();
        // this should never happen
        if (company == null)
            throw new IllegalArgumentException("[" + fortress.getName() + "] has no owner");

        registrationService.isAdminUser(company, "Unable to find requested fortress");

        IFortressUser user = new FortressUser(fortress, fortressUser);
        return save(user);

    }

    @Transactional
    public IFortress registerFortress(FortressInputBean fib) {
        ICompany company = getCompany();

        IFortress fortress = companyService.getFortress(company, fib.getName());
        if (fortress != null) {
            // Already associated, get out of here
            return fortress;
        }

        fortress = new Fortress(fib, company);
        return save(fortress);

    }

    @Transactional
    public IFortress registerFortress(String fortressName) {
        FortressInputBean fb = new FortressInputBean(fortressName, false);
        return registerFortress(fb);
    }

    private ICompany getCompany(long fortressId) {
        return getFortress(fortressId).getCompany();

    }

    public List<IFortress> findFortresses(String companyName) {
        ICompany company = companyService.findByName(companyName);
        if (company == null)
            return null;      //ToDo: what kind of error page to return?
        if (companyService.getAdminUser(company, securityHelper.getUserName(true, true)) != null)
            return fortressDao.findFortresses(company.getId());
        else
            return null; //NotAuth
    }
}
