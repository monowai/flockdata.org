package org.flockdata.authentication;

import org.flockdata.engine.PlatformConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.InMemoryUserDetailsManagerConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import javax.annotation.PostConstruct;

/**
 * Hardcoded users and passwords. Suitable for evaluation and testing
 *
 * You should include the configuration to use this implementation
 *
 * Created by mike on 16/02/16.
 */

@Configuration
@Profile({"fd-auth-test"})
public class AuthTesting implements FdWebSecurity {

    @Autowired
    PlatformConfig engineConfig;

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers(engineConfig.apiBase() +"/login", "/api/ping", "/api/logout", "/api/account").permitAll()
                .antMatchers("/api/v1/**").authenticated()
                .antMatchers("/").permitAll();
        ;

//        http.authorizeRequests()
//                .antMatchers("/**").hasRole("USER")
//                .antMatchers("/admin/**").hasRole("ADMIN");

        //http://www.codesandnotes.be/2015/02/05/spring-securitys-csrf-protection-for-rest-services-the-client-side-and-the-server-side/
        http.csrf().disable();// ToDO: Fix me when we figure out POST/Login issue
        http.httpBasic();


    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        InMemoryUserDetailsManagerConfigurer<AuthenticationManagerBuilder> ima = auth.inMemoryAuthentication();
        ima.withUser("mike")
                .password("123")
                .roles("USER", FdRoles.FD_USER, FdRoles.FD_ADMIN) ;
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
