/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
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


import com.auditbucket.dao.SchemaDao;
import com.auditbucket.engine.service.AbSearchGateway;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.repo.neo4j.dao.CompanyDao;
import com.auditbucket.registration.repo.neo4j.dao.FortressDao;
import com.auditbucket.track.model.DocumentType;
import org.springframework.beans.factory.annotation.Autowired;
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
    private SystemUserService sysUserService;

    @Autowired
    private CompanyDao companyDao;

    @Autowired
    private SchemaDao schemaDao;

    @Autowired
    private SecurityHelper securityHelper;

    public Fortress getFortress(Long id) {
        return fortressDao.findOne(id);
    }

    public FortressUser getUser(Long id) {
        return fortressDao.findOneUser(id);
    }
    @Cacheable(value = "fortressName", unless = "#result == null")
    public Fortress findByName(Company company, String fortressName) {
        return companyDao.getFortressByName(company.getId(), fortressName);
    }


    public Fortress findByName(String fortressName) {
        Company ownedBy = getCompany();
        return findByName(ownedBy, fortressName);
    }

    public Fortress findByCode(String fortressCode) {
        Company ownedBy = getCompany();
        return companyDao.getFortressByCode(ownedBy.getId(), fortressCode);
    }

    private Company getCompany() {
        String userName = securityHelper.getUserName(true, false);
        SystemUser su = sysUserService.findByLogin(userName);
        if (su == null) {
            throw new SecurityException("Invalid user or password");
        }
        return su.getCompany();
    }


    private Fortress save(Company company, FortressInputBean fortress) {
        return fortressDao.save(company, fortress);
    }

    /**
     * Returns an object representing the user in the supplied fortress. User is created
     * if it does not exist
     * <p/>
     * FortressUser Name is deemed to always be unique and is converted to a lowercase trimmed
     * string to enforce this
     *
     *
     * @param company
     * @param fortressUser user to locate
     * @return fortressUser identity
     */
    public FortressUser getFortressUser(Company company, String fortressName, String fortressUser) {
        Fortress fortress = findByName(company, fortressName);
        if ( fortress == null )
            return null;
        return getFortressUser(fortress, fortressUser, true);
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

    @Cacheable(value = "fortressUser", unless = "#result==null" )
    public FortressUser getFortressUser(Fortress fortress, String fortressUser, boolean createIfMissing) {
        if (fortressUser == null || fortress == null)
            throw new IllegalArgumentException("Don't go throwing null in here [" + (fortressUser == null ? "FortressUserNode]" : "FortressNode]"));

        FortressUser fu = fortressDao.getFortressUser(fortress.getId(), fortressUser.toLowerCase());
        if (createIfMissing && fu == null)
            fu = addFortressUser(fortress, fortressUser.toLowerCase().trim());
        return fu;
    }

    private FortressUser addFortressUser(Fortress fortress, String fortressUser) {

        if (fortress == null)
            throw new IllegalArgumentException("Unable to find requested fortress");

        Company company = fortress.getCompany();
        // this should never happen
        if (company == null)
            throw new IllegalArgumentException("[" + fortress.getName() + "] has no owner");

        // If we're dealing with Primary Keys and Objects, we don't need to make security
        //  checks, though this one might be the exception!

        //registrationService.isAdminUser(company, "Unable to find requested fortress");
        return fortressDao.save(fortress, fortressUser);

    }

    public Fortress registerFortress(FortressInputBean fib) {
       return registerFortress(getCompany(), fib, true);

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

    public Collection<Fortress> findFortresses(Company company) {
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

    public void purge(String name) throws DatagioException {
        Fortress fortress = findByName(name);
        if (fortress == null)
            throw new DatagioException("Fortress [" + fortress + "] could not be found");
        fortressDao.delete(fortress);
        String indexName = "ab." + fortress.getCompany().getCode() + "." + fortress.getCode();
        // ToDo: Delete the ES index
        //searchGateway.delete(indexName);
    }

    /**
     * Creates a fortress if it's missing.
     * @param company   who to crate for
     * @param fortressInputBean payload
     * @return existing or newly created fortress
     */
    public Fortress registerFortress(Company company, FortressInputBean fortressInputBean) {
        return registerFortress(company, fortressInputBean, true);
    }

    public Fortress registerFortress(Company company, FortressInputBean fib, boolean createIfMissing) {
        Fortress fortress = companyService.getFortress(company, fib.getName());

        if (fortress != null || !createIfMissing) {
            // Already associated, get out of here
            return fortress;
        }

        fortress = save(company, fib);
        fortress.setCompany(company);
        return fortress;
    }


    public Collection<DocumentType> getFortressDocumentsInUse(Company company, String fortressName) {
        Fortress fortress = findByName(company, fortressName);
        return schemaDao.getFortressDocumentsInUse(fortress);
    }


}
