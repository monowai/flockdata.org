package org.flockdata.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import javax.annotation.PostConstruct;

/**
 * Simple "permitAll" security implementation
 *
 * You should include the profoile configuration to use this implementation
 *
 * Created by mike on 16/02/16.
 */

@Configuration
@Profile({"fd-auth-none"})
public class AuthNone implements FdWebSecurity {

    private static Logger logger = LoggerFactory.getLogger("configuration");

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .anyRequest()
                .permitAll();
        http.httpBasic();
        http.csrf().disable();// ToDO: Fix me when we figure out POST/Login issue
    }

    @PostConstruct
    void dumpConfig() {
        logger.info("**** No authorization is being used");
    }


    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .inMemoryAuthentication()
                    .withUser("mike")
                    .password("123")
                    .roles("USER", FdRoles.FD_USER, FdRoles.FD_ADMIN)

                ;
    }
}
