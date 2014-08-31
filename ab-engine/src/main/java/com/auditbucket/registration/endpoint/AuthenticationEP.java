package com.auditbucket.registration.endpoint;

import com.auditbucket.authentication.LoginRequest;
import com.auditbucket.authentication.UserProfile;
import com.auditbucket.authentication.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class AuthenticationEP {

    @Autowired(required = false)
    @Qualifier("authenticationManager")
    AuthenticationManager authenticationManager;

    @Autowired
    private UserProfileService userProfileService;

    @RequestMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<UserProfile> handleLogin(@RequestBody LoginRequest loginRequest) throws Exception {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                username, password);
        Authentication auth = authenticationManager.authenticate(token);
        SecurityContextHolder.getContext().setAuthentication(auth);
        UserProfile userProfile = userProfileService.getUser(auth);

        return new ResponseEntity<>(userProfile, HttpStatus.OK);
    }

    /**
     * GET  /account -> get the current user.
     */
    @RequestMapping(value = "/account", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<UserProfile> handleLogin() throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        UserProfile userProfile = userProfileService.getUser(auth);
        return new ResponseEntity<>(userProfile, HttpStatus.OK);
    }

    /**
     * GET  /logout -> logout the current user.
     */
    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    @ResponseBody
    public void handleLogout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        SecurityContextHolder.getContext().setAuthentication(null);
    }
}
