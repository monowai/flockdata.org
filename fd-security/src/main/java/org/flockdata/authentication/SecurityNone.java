package org.flockdata.authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * Simple "permitAll" security implementation
 *
 * You should include the profoile configuration to use this implementation
 *
 * Created by mike on 16/02/16.
 */

@Configuration
@Profile({"fd-auth-none"})
public class SecurityNone implements FdWebSecurity {

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .anyRequest()
                .permitAll();
        http.httpBasic();

    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .inMemoryAuthentication()
                    .withUser("mike")
                    .password("123")
                    .roles("USER", FdWebSecurity.ROLE_USER, FdWebSecurity.ROLE_ADMIN)

                ;
    }
}
