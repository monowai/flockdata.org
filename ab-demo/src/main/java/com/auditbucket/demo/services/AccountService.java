package com.auditbucket.demo.services;

import com.auditbucket.bean.AuditResultBean;
import com.auditbucket.demo.domain.Account;
import com.auditbucket.demo.repository.AccountRepository;
import com.auditbucket.spring.AbClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class AccountService {
    public static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    private AbClient abClient;

    public Account saveAccount(Account account) throws IllegalAccessException, IOException {
        logger.debug("Account Created : {}", account.getAccountNumber());
        account = accountRepository.save(account); // Get the PK

        AuditResultBean auditResultBean = abClient.createAuditHeader(account); // ToDo: @Async call
        // ToDo: Following should be in a seperate method
        // ToDo: AuditBucket should talk back to DemoApp via @Gateway so that the account can be updated with the AuditKey
        // ToDo: and potential errors handled here in the client.
        account.setAuditKey(auditResultBean.getAuditKey());
        accountRepository.save(account);
        return account;
    }

    public void updateAccount(Account account) throws IllegalAccessException, IOException {
        Account accountDb = accountRepository.findByAccountNumber(account.getAccountNumber());
        accountDb.setStatus(account.getStatus());
        accountRepository.save(accountDb);
        abClient.createAuditLog(accountDb);
    }

}
