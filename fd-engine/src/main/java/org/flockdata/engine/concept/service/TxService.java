/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

package org.flockdata.engine.concept.service;

import java.util.Map;
import java.util.Set;
import org.flockdata.authentication.SecurityHelper;
import org.flockdata.authentication.SystemUserService;
import org.flockdata.data.SystemUser;
import org.flockdata.data.TxRef;
import org.flockdata.engine.data.dao.EntityDaoNeo;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.integration.KeyGenService;
import org.flockdata.track.bean.ContentInputBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author mholdsworth
 * @since 4/09/2014
 */
@Service
@Transactional
public class TxService {

  @Autowired
  SystemUserService sysUserService;
  @Autowired
  EntityDaoNeo trackDao;
  @Autowired
  private KeyGenService keyGenService;
  @Autowired
  private SecurityHelper securityHelper;

  TxRef beginTransaction(CompanyNode company) {
    return beginTransaction(keyGenService.getUniqueKey(), company);
  }

  TxRef beginTransaction(String id, CompanyNode company) {
    return trackDao.beginTransaction(id, company);

  }

  public Map<String, Object> findByTXRef(String txRef) {
    TxRef tx = findTx(txRef);
    return (tx == null ? null : trackDao.findByTransaction(tx));
  }

  public TxRef handleTxRef(ContentInputBean input, CompanyNode company) {
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

    if (su == null) {
      throw new SecurityException("Not authorised");
    }
    TxRef tx = trackDao.findTxTag(txRef, su.getCompany());
    if (tx == null) {
      return null;
    }
    return tx;
  }

  public Set<EntityNode> findTxEntities(String txName) {
    TxRef txRef = findTx(txName);
    if (txRef == null) {
      return null;
    }
    return trackDao.findEntitiesByTxRef(txRef.getId());
  }

}
