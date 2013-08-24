package com.auditbucket.demo.services;

import com.auditbucket.demo.domain.Account;
import com.auditbucket.demo.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    @Autowired
    AccountRepository accountRepository;

    public void saveAccount(Account account){
        accountRepository.save(account);
    }

}
