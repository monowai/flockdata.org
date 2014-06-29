package com.auditbucket.authentication.endpoint;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.auditbucket.authentication.model.LoginRequest;
import com.auditbucket.authentication.model.User;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.service.SystemUserService;
import com.stormpath.spring.security.provider.StormpathUserDetails;

@Controller
public class AuthenticationEP {
	private static final Logger logger = LoggerFactory
			.getLogger(AuthenticationEP.class);

	@Autowired(required = false)
	@Qualifier("authenticationManager")
	AuthenticationManager authenticationManager;

    @Autowired
    private SystemUserService systemUserService;

    @RequestMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<User> handleLogin(@RequestBody LoginRequest loginRequest) throws Exception {
		String username = loginRequest.getUsername();
		String password = loginRequest.getPassword();
		
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
				username, password);
		try {
			Authentication auth = authenticationManager.authenticate(token);
			SecurityContextHolder.getContext().setAuthentication(auth);
			StormpathUserDetails stormpathUser = (StormpathUserDetails) auth.getPrincipal();
			
			logger.info("User Properties - " + stormpathUser.getProperties());
			logger.info("Authorities - " + auth.getAuthorities());
			
			User user = new User();
			user.setUserId(username);
			user.setUserName(stormpathUser.getUsername());
			user.setUserEmail(stormpathUser.getProperties().get("email"));
			user.setStatus(stormpathUser.getProperties().get("status"));
			
			if(!auth.getAuthorities().isEmpty()) {
				Iterator<? extends GrantedAuthority> roles = auth.getAuthorities().iterator();
				while(roles.hasNext()) {
					user.addUserRole(roles.next().getAuthority());
				}
			}
			
			SystemUser sysUser = systemUserService.findByLogin(username);
			if(sysUser != null) {
				user.setApiKey(sysUser.getApiKey());
			}
			
			return new ResponseEntity<User>(user, HttpStatus.OK);
		} catch (BadCredentialsException e) {
			return new ResponseEntity<User>(HttpStatus.UNAUTHORIZED);
		}
	}

}
