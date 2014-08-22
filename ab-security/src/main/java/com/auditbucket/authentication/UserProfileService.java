package com.auditbucket.authentication;

import org.springframework.security.core.Authentication;

public interface UserProfileService {

	public UserProfile getUser(Authentication authentication);

    public String getProvider();
}
