package org.flockdata.authentication;

import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * Created by mike on 16/02/16.
 */
public interface FdWebSecurity {

    String USER = "FD_USER";
    String ADMIN = "FD_ADMIN";
    String ROLE_ADMIN = "ROLE_FD_ADMIN";
    String ROLE_USER = "ROLE_FD_USER";

    void configure(HttpSecurity http) throws Exception;

    void configureGlobal(AuthenticationManagerBuilder auth) throws Exception;
}
