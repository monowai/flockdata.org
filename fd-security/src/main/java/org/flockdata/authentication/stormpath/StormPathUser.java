package org.flockdata.authentication.stormpath;

import com.stormpath.spring.security.provider.StormpathUserDetails;
import org.flockdata.authentication.UserProfile;
import org.flockdata.authentication.UserProfileService;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.registration.service.SystemUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class StormPathUser implements UserProfileService {

    private static final Logger logger = LoggerFactory
            .getLogger(StormPathUser.class);

    static {
        logger.info("Using Stormpath profile service");
    }

    @Autowired
    private SystemUserService systemUserService;

    @Override
    public UserProfile getUser(Authentication auth) {
        StormpathUserDetails stormpathUser = (StormpathUserDetails) auth.getPrincipal();

        UserProfile userProfile = new UserProfile();
        String userEmail = stormpathUser.getProperties().get("email");
        String userId = stormpathUser.getProperties().get("username");

        userProfile.setUserId(userId);
        userProfile.setUserName(stormpathUser.getUsername());
        userProfile.setUserEmail(userEmail);
        userProfile.setStatus(stormpathUser.getProperties().get("status"));

        if (!auth.getAuthorities().isEmpty()) {
            for (
                    GrantedAuthority grantedAuthority : auth.getAuthorities()) {
                userProfile.addUserRole(grantedAuthority.getAuthority());
            }
        }

        SystemUser sysUser = systemUserService.findByLogin(userId);
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
