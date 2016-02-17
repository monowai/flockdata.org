package org.flockdata.authentication.simple;

import org.flockdata.authentication.UserProfile;
import org.flockdata.authentication.UserProfileService;
import org.flockdata.authentication.registration.service.SystemUserService;
import org.flockdata.model.SystemUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

@Component
public class SimpleUser implements UserProfileService{
	@Autowired  (required = false)
	private SystemUserService systemUserService;

    private static Logger logger = LoggerFactory.getLogger("configuration");

	public UserProfile getUser(Authentication authentication) {
        Object userName = authentication.getPrincipal();
        String login;
        User auth = null;
        if ( userName instanceof String )
            login = (String)userName;
        else {
            login = ((User)authentication.getPrincipal()).getUsername();
            auth = (User) authentication.getPrincipal();
        }

        UserProfile userProfile = new UserProfile();
		userProfile.setUserId(login);
		userProfile.setStatus("ENABLED");

		if (auth!=null && !auth.getAuthorities().isEmpty()) {
            for (GrantedAuthority grantedAuthority : auth.getAuthorities()) {
                userProfile.addUserRole(grantedAuthority.getAuthority());
            }
		}
        if ( auth!=null && systemUserService !=null ) {
            SystemUser sysUser = systemUserService.findByLogin(login);
            if (sysUser != null) {
                userProfile.setApiKey(sysUser.getApiKey());
                userProfile.setCompany(sysUser.getCompany().getName());
            }
        }

		return userProfile;
	}

    public String getProvider() {
        return "Simple SimpleUser-Security";
    }

}
