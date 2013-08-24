package com.auditbucket.demo.services;

import com.auditbucket.demo.domain.Account;
import com.auditbucket.demo.repository.AccountRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(locations = { "classpath:applicationContext-test.xml"})
public class AccountServiceTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    public void testSaveAccount(){
        Account account = new Account();
        account.setNrCompte("numAccount");
        account.setIban("iban");
        account.setRib("rib");
        account.setCodeAgence("codeagence");
        accountRepository.save(account);
        Assert.assertEquals(accountRepository.count(),1);
    }
}
