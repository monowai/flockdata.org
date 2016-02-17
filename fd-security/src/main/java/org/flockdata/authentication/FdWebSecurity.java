package org.flockdata.authentication;

import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * Created by mike on 16/02/16.
 */
public interface FdWebSecurity {

    void configure(HttpSecurity http) throws Exception;

    void configureGlobal(AuthenticationManagerBuilder auth) throws Exception;
}
