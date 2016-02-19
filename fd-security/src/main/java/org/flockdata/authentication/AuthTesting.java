package org.flockdata.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.InMemoryUserDetailsManagerConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import javax.annotation.PostConstruct;

/**
 * Hardcoded users and passwords. Suitable for evaluation and testing
 * <p>
 * You should include the configuration to use this implementation
 * <p>
 * Created by mike on 16/02/16.
 */

@Configuration
@Profile({"fd-auth-test"}) //
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class AuthTesting extends WebSecurityConfigurerAdapter {

    @Configuration
    @Order(10) // Preventing clash with AuthTesting deployment (100)
    public static class ApiSecurity extends WebSecurityConfigurerAdapter {

        @Override
        public void configure(HttpSecurity http) throws Exception {
            // Security in FD take place at the service level so acess to all endpoints is granted
            // ApiKeyInterceptor is a part of the auth chain

            http.authorizeRequests()
                .antMatchers("/api/login", "/api/ping", "/api/logout", "/api/account").permitAll()
                .antMatchers("/api/v1/**").permitAll()
                    .antMatchers("/").permitAll()
            ;


            //http://www.codesandnotes.be/2015/02/05/spring-securitys-csrf-protection-for-rest-services-the-client-side-and-the-server-side/
            http.csrf().disable();// ToDO: Fix me when we figure out POST/Login issue
            http.httpBasic();
        }
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        InMemoryUserDetailsManagerConfigurer<AuthenticationManagerBuilder> ima = auth.inMemoryAuthentication();
        ima.withUser("mike")
                .password("123")
                .roles("USER", FdRoles.FD_USER, FdRoles.FD_ADMIN);
        ima.withUser("sally")
                .password("123")
                .roles("USER", FdRoles.FD_USER, FdRoles.FD_ADMIN);
        ima.withUser("harry")
                .password("123")
                .roles("USER", FdRoles.FD_USER);

    }

    private static Logger logger = LoggerFactory.getLogger("configuration");

    @PostConstruct
    void dumpConfig() {
        logger.info("**** Limited authorization (for testing) is being used");
    }

}
