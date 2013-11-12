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


import com.auditbucket.engine.service.AbSearchGateway;
import com.auditbucket.helper.AuditException;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.repo.neo4j.dao.CompanyDao;
import com.auditbucket.registration.repo.neo4j.dao.FortressDao;
import com.auditbucket.registration.repo.neo4j.model.FortressNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
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

    @Cacheable(value = "fortressName", unless = "#result == null")
    public Fortress findByName(String fortressName) {
        Company ownedBy = getCompany();
        return companyDao.getFortressByName(ownedBy.getId(), fortressName);
    }

    public Fortress findByCode(String fortressCode) {
        Company ownedBy = getCompany();
        return companyDao.getFortressByCode(ownedBy.getId(), fortressCode);
    }

    private Company getCompany() {
        String userName = securityHelper.getUserName(true, false);
        SystemUser su = sysUserService.findByName(userName);
        if (su == null) {
            throw new SecurityException("Invalid user or password");
        }
        return su.getCompany();
    }

    @CacheEvict(value = "fortressName", key = "#p0.id")
    public Fortress save(Fortress fortress) {
        return fortressDao.save(fortress);
    }

    /**
     * Returns an object representing the user in the supplied fortress. User is created
     * if it does not exist
     * <p/>
     * FortressUser Name is deemed to always be unique and is converted to a lowercase trimmed
     * string to enforce this
     *
     * @param fortress     fortress to search
     * @param fortressUser user to locate
     * @return fortressUser identity
     */
    public FortressUser getFortressUser(Fortress fortress, String fortressUser) {
        return getFortressUser(fortress, fortressUser, true);
    }

    @Cacheable(value = "fortressUser")
    public FortressUser getFortressUser(Fortress fortress, String fortressUser, boolean createIfMissing) {
        if (fortressUser == null || fortress == null)
            throw new IllegalArgumentException("Don't go throwing null in here [" + (fortressUser == null ? "FortressUserNode]" : "FortressNode]"));

        FortressUser fu = fortressDao.getFortressUser(fortress.getId(), fortressUser.toLowerCase());
        if (createIfMissing && fu == null)
            fu = addFortressUser(fortress.getId(), fortressUser.toLowerCase().trim());
        return fu;
    }

    private FortressUser addFortressUser(Long fortressId, String fortressUser) {

        Fortress fortress = getFortress(fortressId);
        if (fortress == null)
            throw new IllegalArgumentException("Unable to find requested fortress");


        Company company = fortress.getCompany();
        // this should never happen
        if (company == null)
            throw new IllegalArgumentException("[" + fortress.getName() + "] has no owner");

        registrationService.isAdminUser(company, "Unable to find requested fortress");
        return fortressDao.save(fortress, fortressUser);

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

    public Collection<Fortress> findFortresses() {
        Company company = securityHelper.getCompany();
        if (company == null)
            return new ArrayList<>();
        return fortressDao.findFortresses(company.getId());

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

    public void fetch(FortressUser lastUser) {
        fortressDao.fetch(lastUser);

    }

    @Autowired
    AbSearchGateway searchGateway;

    public void purge(String name) throws AuditException {
        Fortress fortress = findByName(name);
        if (fortress == null)
            throw new AuditException("Fortress [" + fortress + "] could not be found");
        fortressDao.delete(fortress);
        String indexName = "ab." + fortress.getCompany().getCode() + "." + fortress.getCode();
        // ToDo: Delete the ES index
        //searchGateway.delete(indexName);
    }

}
