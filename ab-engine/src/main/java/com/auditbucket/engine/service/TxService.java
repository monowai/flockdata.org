package com.auditbucket.engine.service;

import com.auditbucket.engine.repo.neo4j.EntityDaoNeo;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.service.KeyGenService;
import com.auditbucket.registration.service.SystemUserService;
import com.auditbucket.track.bean.ContentInputBean;
import com.auditbucket.track.model.Entity;
import com.auditbucket.track.model.TxRef;
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
        TxRef tx = trackDao.findTxTag(txRef, su.getCompany(), fetchHeaders);
        if (tx == null)
            return null;
        return tx;
    }

    public Set<Entity> findTxHeaders(String txName) {
        TxRef txRef = findTx(txName);
        if (txRef == null)
            return null;
        return trackDao.findHeadersByTxRef(txRef.getId());
    }

}
