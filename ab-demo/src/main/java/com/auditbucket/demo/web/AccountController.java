package com.auditbucket.demo.web;

import com.auditbucket.demo.domain.Account;
import com.auditbucket.demo.services.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

@Controller
@RequestMapping(value = "/account")
public class AccountController {
    //private static final Logger LOGGER = LoggerFactory.getLogger(AccountController.class);
    @Autowired
    private AccountService accountService;

    @RequestMapping(value = "/ping", method = RequestMethod.GET)
    @ResponseBody
    public String get() throws Exception {
        // curl -u mike:123 -X GET http://localhost:8080/ab/audit/ping
        return "Pong!";
    }

    @ResponseBody
    @RequestMapping(value = "/save",
            produces = "application/json",
            consumes = "application/json",
            method = RequestMethod.POST)
    public ResponseEntity save(@RequestBody Account account) {
        try {
            Account accountDb = accountService.saveAccount(account);
            return new ResponseEntity<>(accountDb, HttpStatus.OK);
        } catch (IllegalAccessException e) {
            return new ResponseEntity<>("Log Auditing failed", HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            return new ResponseEntity<>("Log Auditing failed", HttpStatus.BAD_REQUEST);
        }

    }

    @RequestMapping(value = "/update", produces = "application/json", consumes = "application/json", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> update(@RequestBody Account account) {
        try {
            accountService.updateAccount(account);
            return new ResponseEntity<>("Account Updated", HttpStatus.OK);
        } catch (IllegalAccessException e) {
            return new ResponseEntity<>("Log Auditing failed", HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            return new ResponseEntity<>("Log Auditing failed", HttpStatus.BAD_REQUEST);
        }
    }

}
