package com.auditbucket.authentication.provider;

import com.auditbucket.authentication.UserProfile;
import com.auditbucket.authentication.UserProfileService;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.service.SystemUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;


public class Spring implements UserProfileService {
	@Autowired
	private SystemUserService systemUserService;

    private static final Logger logger = LoggerFactory
            .getLogger(Spring.class);

    static  {
        logger.info("Using Spring-Security simple service");
    }

    @Override
	public UserProfile getUser(Authentication authentication) {
		User auth = (User) authentication.getPrincipal();
		UserProfile userProfile = new UserProfile();
		userProfile.setUserId(auth.getUsername());
		userProfile.setStatus("ENABLED");

		if (!auth.getAuthorities().isEmpty()) {
            for (GrantedAuthority grantedAuthority : auth.getAuthorities()) {
                userProfile.addUserRole(grantedAuthority.getAuthority());
            }
		}

		SystemUser sysUser = systemUserService.findByLogin(auth.getUsername());
		if (sysUser != null) {
			userProfile.setApiKey(sysUser.getApiKey());
			userProfile.setCompany(sysUser.getCompany().getName());
		}

		return userProfile;
	}

    @Override
    public String getProvider() {
        return "Simple Spring-Security";
    }

}
