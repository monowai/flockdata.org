package com.auditbucket.authentication.service;

import org.springframework.security.core.Authentication;

import com.auditbucket.authentication.model.User;

public interface UserProfileService {

	public User getUser(Authentication authentication);
}
