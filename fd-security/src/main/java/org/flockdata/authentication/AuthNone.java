package org.flockdata.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.annotation.PostConstruct;

/**
 * Simple "permitAll" security implementation
 * <p>
 * You should include the profoile configuration to use this implementation
 * <p>
 * Created by mike on 16/02/16.
 */

@Configuration
@Profile({"fd-auth-none"})
public class AuthNone extends WebMvcConfigurerAdapter {

    private static Logger logger = LoggerFactory.getLogger("configuration");

    @Configuration
    @Order(20) // Preventing clash with AuthTesting
    public static class ApiSecurity extends WebSecurityConfigurerAdapter {

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http.authorizeRequests()
                    .antMatchers("/").permitAll()
            ;
            //http://www.codesandnotes.be/2015/02/05/spring-securitys-csrf-protection-for-rest-services-the-client-side-and-the-server-side/
            http.csrf().disable();// ToDO: Fix me when we figure out POST/Login issue
            http.httpBasic();
        }
    }

    @PostConstruct
    void dumpConfig() {
        logger.info("**** No authorization is being used");
    }


    @Autowired
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .inMemoryAuthentication()
                .withUser("mike")
                .password("123")
                .roles("USER", FdRoles.FD_USER, FdRoles.FD_ADMIN)

        ;
    }
}
