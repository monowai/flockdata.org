/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.                                                  f
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.engine.service;


import com.auditbucket.engine.repo.neo4j.dao.FortressDao;
import com.auditbucket.engine.repo.neo4j.dao.SchemaDaoNeo4j;
import com.auditbucket.helper.FlockException;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.service.SystemUserService;
import com.auditbucket.track.bean.DocumentResultBean;
import com.auditbucket.track.model.DocumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;

@Service
@Transactional
public class FortressService {
    private Logger logger = LoggerFactory.getLogger(FortressService.class);
    @Autowired
    private FortressDao fortressDao;

    @Autowired
    private SystemUserService sysUserService;

    @Autowired
    private SchemaDaoNeo4j schemaDao;

    @Autowired
    private SecurityHelper securityHelper;

    public Fortress getFortress(Long id) {
        return fortressDao.findOne(id);
    }

    public FortressUser getUser(Long id) {
        return fortressDao.findOneUser(id);
    }
//    @Cacheable(value = "fortressName", unless = "#result == null")
    public Fortress findByName(Company company, String fortressName) {
        return fortressDao.getFortressByName(company.getId(), fortressName);
    }


    public Fortress findByName(String fortressName) {
        Company ownedBy = getCompany();
        return findByName(ownedBy, fortressName);
    }

    public Fortress findByCode(String fortressCode) {
        Company ownedBy = getCompany();
        return findByCode(ownedBy, fortressCode) ;
    }

    public Fortress findByCode(Company company, String fortressCode) {
        return fortressDao.getFortressByCode(company.getId(), fortressCode);
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
     * @param company pre-authorised company
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

    //    @Cacheable(value = "fortressUser", unless = "#result==null" )
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
        logger.trace("Request to add fortressUser [{}], [{}]",fortress, fortressUser);
        Fortress fortressz = findByCode(fortress.getCompany(), fortress.getCode());
        logger.trace("Fortress result {}", fortressz);
        Company company = fortress.getCompany();
        // this should never happen
        if (company == null)
            throw new IllegalArgumentException("[" + fortress.getName() + "] has no owner");

        // If we're dealing with Primary Keys and Objects, we don't need to make security
        //  checks, though this one might be the exception!

        //registrationService.isAdminUser(company, "Unable to find requested fortress");
        return fortressDao.save(fortress, fortressUser);

    }

    @Deprecated
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
    @Deprecated
    public Fortress registerFortress(String fortressName) {
        FortressInputBean fb = new FortressInputBean(fortressName, true);
        return registerFortress(fb);
    }

    public Collection<Fortress> findFortresses() throws FlockException {
        Company company = securityHelper.getCompany();
        if (company == null)
            return new ArrayList<>();
        return findFortresses(company);

    }

    public Collection<Fortress> findFortresses(Company company) throws FlockException {
        if ( company == null )
            throw new FlockException("Unable to identify the requested company");
        return fortressDao.findFortresses(company.getId());

    }

    public void fetch(FortressUser lastUser) {
        fortressDao.fetch(lastUser);

    }

    public void purge(Fortress fortress) throws FlockException {
        fortressDao.delete(fortress);
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
        logger.debug("Fortress registration request {}, {}", company, fib);
        Fortress fortress = fortressDao.getFortressByName(company.getId(), fib.getName());

        if (fortress != null || !createIfMissing) {
            // Already associated, get out of here
            logger.debug("Company {} has existing fortress {}", company, fortress);
            return fortress;
        }

        fortress = save(company, fib);
        logger.debug ("Created fortress {}", fortress);
        fortress.setCompany(company);
        logger.debug("Returning fortress {}", fortress);
        return fortress;
    }


    public Collection<DocumentResultBean> getFortressDocumentsInUse(Company company, String code) {
        Fortress fortress = findByCode(company, code);
        if ( fortress == null )
            fortress = findByName(company, code);
        if (fortress == null ) {
            return new ArrayList<>();
        }
        Collection<DocumentResultBean>results = new ArrayList<>();
        Collection<DocumentType> rawDocs = schemaDao.getFortressDocumentsInUse(fortress);
        for (DocumentType rawDoc : rawDocs) {
            results.add(new DocumentResultBean(rawDoc));
        }
        return results;
    }


}
