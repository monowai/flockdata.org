/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.helper;

import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * User: Mike Holdsworth
 * Date: 17/04/13
 * Time: 10:11 PM
 */
@Service
public class SecurityHelper {
    @Autowired
    private SystemUserService sysUserService;

    public String isValidUser() {
        return getUserName(true, true);
    }

    public String getLoggedInUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null)
            throw new SecurityException("User is not authenticated");
        return a.getName();
    }

    public String getUserName(boolean exceptionOnNull, boolean isSysUser) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null)
            if (exceptionOnNull)
                throw new SecurityException("User is not authenticated");
            else
                return null;

        if (isSysUser) {
            SystemUser su = getSysUser(a.getName());
            if (su == null)
                throw new IllegalArgumentException("Not authorised");
        }
        return a.getName();
    }

    public SystemUser getSysUser(boolean exceptionOnNull) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null)
            if (exceptionOnNull)
                throw new SecurityException("User is not authenticated");
            else
                return null;

        return sysUserService.findByName(a.getName());
    }

    SystemUser getSysUser(String loginName) {
        return sysUserService.findByName(loginName);
    }

    public Company getCompany() {
        String userName = getLoggedInUser();
        SystemUser su = sysUserService.findByName(userName);

        if (su == null)
            throw new SecurityException("Not authorised");

        return su.getCompany();
    }

    public Company getCompany(String apiKey) {
        if (apiKey == null)
            return getCompany();

        SystemUser su = sysUserService.findByApiKey(apiKey);
        if ( su == null )
            return null;
        return su.getCompany();
    }
}
