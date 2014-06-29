package com.auditbucket.authentication.service;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import com.auditbucket.authentication.model.User;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.service.SystemUserService;
import com.stormpath.spring.security.provider.StormpathUserDetails;

public class StormpathUserProfile implements UserProfileService {

	private static final Logger logger = LoggerFactory
			.getLogger(StormpathUserProfile.class);

    @Autowired
    private SystemUserService systemUserService;

	@Override
	public User getUser(Authentication auth) {
		StormpathUserDetails stormpathUser = (StormpathUserDetails) auth.getPrincipal();
		
		logger.info("User Properties - " + stormpathUser.getProperties());
		logger.info("Authorities - " + auth.getAuthorities());
		
		User user = new User();
		String userEmail = stormpathUser.getProperties().get("email");

		user.setUserId(userEmail);
		user.setUserName(stormpathUser.getUsername());
		user.setUserEmail(userEmail);
		user.setStatus(stormpathUser.getProperties().get("status"));
		
		if(!auth.getAuthorities().isEmpty()) {
			Iterator<? extends GrantedAuthority> roles = auth.getAuthorities().iterator();
			while(roles.hasNext()) {
				user.addUserRole(roles.next().getAuthority());
			}
		}
		
		SystemUser sysUser = systemUserService.findByLogin(userEmail);
		if(sysUser != null) {
			user.setApiKey(sysUser.getApiKey());
			user.setCompany(sysUser.getCompany().getName());
		}
		
		return user;
	}

}
