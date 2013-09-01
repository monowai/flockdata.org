package com.auditbucket.demo.services;

import com.auditbucket.demo.domain.Account;
import com.auditbucket.demo.repository.AccountRepository;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(locations = {"classpath:applicationContext-test.xml"})
public class AccountServiceTest {

    public static Logger logger = LoggerFactory.getLogger(AccountServiceTest.class);
    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountRepository accountRepository;

    //@Ignore
    @Test
    public void testSaveAccount() throws IllegalAccessException, IOException {
        Account account = new Account();
        account.setAccountNumber("numAccount");
        account.setIban("iban");
        account.setStatus("CREATED");
        accountService.saveAccount(account);

        Account accountDb = accountRepository.findByAccountNumber("numAccount");
        Assert.assertNotNull(accountDb);
        Assert.assertEquals(accountDb.getAccountNumber(), "numAccount");
        Assert.assertEquals(accountDb.getIban(), "iban");
        Assert.assertNotNull(accountDb.getAuditKey());
        logger.info("Audit Key Generated By Ab Engine is : {}", accountDb.getAuditKey());
    }

    @Ignore
    @Test
    public void testUpdateAccount() throws IllegalAccessException, IOException {

        Account account = new Account();
        account.setAccountNumber("numAccount1");
        account.setIban("iban1");
        accountService.saveAccount(account);

        Account accountDb = accountRepository.findByAccountNumber("numAccount1");
        Assert.assertNotNull(accountDb);
        Assert.assertEquals(accountDb.getAccountNumber(), "numAccount1");
        Assert.assertEquals(accountDb.getIban(), "iban1");
        Assert.assertNotNull(accountDb.getAuditKey());
        logger.info("Audit Key Generated By Ab Engine is : {}", accountDb.getAuditKey());

        // Updating Account with numberAccount =  numAccount

        Account accountForUpdate = new Account();
        accountForUpdate.setAccountNumber("numAccount1");
        accountForUpdate.setStatus("UPDATED");
        accountService.updateAccount(accountForUpdate);

    }
}
