package com.auditbucket.authentication.service;

import java.util.Iterator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import com.auditbucket.authentication.model.User;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.service.SystemUserService;

public class SpringUserProfile implements UserProfileService {
	@Autowired
	private SystemUserService systemUserService;

	@Override
	public User getUser(Authentication authentication) {
		org.springframework.security.core.userdetails.User auth = (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
		User user = new User();
		user.setUserId(auth.getUsername());
		user.setStatus("ENABLED");

		if (!auth.getAuthorities().isEmpty()) {
			Iterator<? extends GrantedAuthority> roles = auth.getAuthorities()
					.iterator();
			while (roles.hasNext()) {
				user.addUserRole(roles.next().getAuthority());
			}
		}

		SystemUser sysUser = systemUserService.findByLogin(auth.getUsername());
		if (sysUser != null) {
			user.setApiKey(sysUser.getApiKey());
			user.setCompany(sysUser.getCompany().getName());
		}

		return user;
	}

}
