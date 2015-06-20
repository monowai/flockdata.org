/*
 * Copyright (c) 2012-2015 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.concept.service;

import org.flockdata.registration.model.SystemUser;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.engine.track.EntityDaoNeo;
import org.flockdata.helper.SecurityHelper;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.service.KeyGenService;
import org.flockdata.registration.service.SystemUserService;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.TxRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

/**
 * User: mike
 * Date: 4/09/14
 * Time: 4:23 PM
 */
@Service
@Transactional
public class TxService {

    @Autowired
    private KeyGenService keyGenService;


    @Autowired
    SystemUserService sysUserService;

    @Autowired
    private SecurityHelper securityHelper;

    @Autowired
    EntityDaoNeo trackDao;

    TxRef beginTransaction(Company company) {
        return beginTransaction(keyGenService.getUniqueKey(), company);
    }

    TxRef beginTransaction(String id, Company company) {
        return trackDao.beginTransaction(id, company);

    }

    public Map<String, Object> findByTXRef(String txRef) {
        TxRef tx = findTx(txRef);
        return (tx == null ? null : trackDao.findByTransaction(tx));
    }
    public TxRef handleTxRef(ContentInputBean input, Company company) {
        TxRef txRef = null;
        if (input.isTransactional()) {
            if (input.getTxRef() == null) {
                txRef = beginTransaction(company);
                input.setTxRef(txRef.getName());
            } else {
                txRef = beginTransaction(input.getTxRef(), company);
            }
        }

        return txRef;
    }

    public TxRef findTx(String txRef) {
        return findTx(txRef, false);
    }

    TxRef findTx(String txRef, boolean fetchHeaders) {
        String userName = securityHelper.getLoggedInUser();
        SystemUser su = sysUserService.findByLogin(userName);

        if (su == null)
            throw new SecurityException("Not authorised");
        TxRef tx = trackDao.findTxTag(txRef, su.getCompany());
        if (tx == null)
            return null;
        return tx;
    }

    public Set<Entity> findTxEntities(String txName) {
        TxRef txRef = findTx(txName);
        if (txRef == null)
            return null;
        return trackDao.findEntitiesByTxRef(txRef.getId());
    }

}
