package com.auditbucket.authentication.service;

import com.auditbucket.authentication.UserProfile;
import org.springframework.security.core.Authentication;

public interface UserProfileService {

	public UserProfile getUser(Authentication authentication);
}
