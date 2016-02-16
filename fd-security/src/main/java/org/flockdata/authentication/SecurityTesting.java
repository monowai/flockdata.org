package org.flockdata.authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.InMemoryUserDetailsManagerConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * Simple "permitAll" security implementation
 *
 * You should include the profoile configuration to use this implementation
 *
 * Created by mike on 16/02/16.
 */

@Configuration
@Profile("fd-auth-test")
public class SecurityTesting implements FdWebSecurity {

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .anyRequest()
                .authenticated();
        http.httpBasic();

    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        InMemoryUserDetailsManagerConfigurer<AuthenticationManagerBuilder> ima = auth.inMemoryAuthentication();
        ima.withUser("mike")
                .password("123")
                .roles("USER", FdWebSecurity.USER, FdWebSecurity.ADMIN) ;
        ima.withUser("sally")
                .password("123")
                .roles("USER", FdWebSecurity.USER, FdWebSecurity.ADMIN);
        ima.withUser("harry")
                .password("123")
                .roles("USER", FdWebSecurity.USER, FdWebSecurity.ADMIN);

    }
}
