package org.flockdata.authentication;

import org.springframework.security.core.Authentication;

public interface UserProfileService {

	UserProfile getUser(Authentication authentication);

    String getProvider();
}
