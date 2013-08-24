package com.auditbucket.demo.web;

import com.auditbucket.demo.domain.Account;
import com.auditbucket.demo.services.AccountService;
import com.auditbucket.spring.AbClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/account")
public class AccountController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountController.class);
    @Autowired
    private AccountService accountService;

    @Autowired
    private AbClient abClient;

    @RequestMapping(value = "/save", produces = "application/json", consumes = "application/json", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> save(@RequestBody Account account) {
        accountService.saveAccount(account);
        //abClient.createAuditHeader(account);
        return new ResponseEntity<String>("Account Created", HttpStatus.OK);
    }

    @RequestMapping(value = "/save", produces = "application/json", consumes = "application/json", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> update(@RequestBody Account account) {
        accountService.saveAccount(account);
        return new ResponseEntity<String>("Account Updated", HttpStatus.OK);
    }

}
