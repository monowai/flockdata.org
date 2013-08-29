package com.auditbucket.demo.services;

import com.auditbucket.bean.AuditLogInputBean;
import com.auditbucket.bean.AuditResultBean;
import com.auditbucket.demo.domain.Account;
import com.auditbucket.demo.repository.AccountRepository;
import com.auditbucket.spring.AbClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    public static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    private AbClient abClient;

    public void saveAccount(Account account) throws IllegalAccessException {
        logger.info("Account Created : "+account.getNrCompte());
        AuditResultBean auditResultBean = abClient.createAuditHeader(account);
        account.setAuditKey(auditResultBean.getAuditKey());
        accountRepository.save(account);
    }

//    public void updateAccount(Account account) throws IllegalAccessException {
//        Account accountDb = accountRepository.findByNrCompte(account.getNrCompte());
//        //accountDb
//        AuditLogInputBean auditLogInputBean = abClient.createLogHeader(account);
//        accountRepository.save(account);
//    }

}
