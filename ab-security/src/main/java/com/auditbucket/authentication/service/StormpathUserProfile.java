package com.auditbucket.authentication.service;

import com.auditbucket.authentication.UserProfile;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.service.SystemUserService;
import com.stormpath.spring.security.provider.StormpathUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class StormpathUserProfile implements UserProfileService {

    private static final Logger logger = LoggerFactory
            .getLogger(StormpathUserProfile.class);

    @Autowired
    private SystemUserService systemUserService;

    @Override
    public UserProfile getUser(Authentication auth) {
        // ToDo: this should not be an implementation specific class
        //          possibly a Map<String,Object>
        StormpathUserDetails stormpathUser = (StormpathUserDetails) auth.getPrincipal();

        logger.info("UserProfile Properties - " + stormpathUser.getProperties());
        logger.info("Authorities - " + auth.getAuthorities());

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

}
