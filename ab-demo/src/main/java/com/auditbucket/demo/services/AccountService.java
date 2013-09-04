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
        logger.info("Account Created : {}", account.getAccountNumber());
        AuditResultBean auditResultBean = abClient.createAuditHeader(account);
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
