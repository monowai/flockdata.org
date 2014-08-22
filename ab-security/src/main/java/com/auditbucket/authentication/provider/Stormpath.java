package com.auditbucket.authentication.provider;

import com.auditbucket.authentication.UserProfile;
import com.auditbucket.authentication.UserProfileService;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.service.SystemUserService;
import com.stormpath.spring.security.provider.StormpathUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class Stormpath implements UserProfileService {

    private static final Logger logger = LoggerFactory
            .getLogger(Stormpath.class);

    static  {
        logger.info("Using Stormpath profile service");
    }
    @Autowired
    private SystemUserService systemUserService;

    @Override
    public UserProfile getUser(Authentication auth) {
        StormpathUserDetails stormpathUser = (StormpathUserDetails) auth.getPrincipal();

        //logger.debug("UserProfile Properties - " + stormpathUser.getProperties());
        //logger.debug("Authorities - " + auth.getAuthorities());

        UserProfile userProfile = new UserProfile();
        String userEmail = stormpathUser.getProperties().get("email");

        userProfile.setUserId(userEmail);
        userProfile.setUserName(stormpathUser.getUsername());
        userProfile.setUserEmail(userEmail);
        userProfile.setStatus(stormpathUser.getProperties().get("status"));

        if (!auth.getAuthorities().isEmpty()) {
            for (
                    GrantedAuthority grantedAuthority : auth.getAuthorities()) {
                userProfile.addUserRole(grantedAuthority.getAuthority());
            }
        }

        SystemUser sysUser = systemUserService.findByLogin(userEmail);
        if (sysUser != null) {
            userProfile.setApiKey(sysUser.getApiKey());
            userProfile.setCompany(sysUser.getCompany().getName());
        }

        return userProfile;
    }

    @Override
    public String getProvider() {
        return "Stormpath";
    }

}
