/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.helper;

import org.flockdata.configure.SecurityHelper;
import org.flockdata.model.Company;
import org.flockdata.model.SystemUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ApiKeyInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory
            .getLogger(ApiKeyInterceptor.class);
    public static final String COMPANY = "company";
    public static final String API_KEY = "api-key";

    @Autowired
    private SecurityHelper securityHelper;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler) throws Exception {
        String apiKey = request.getHeader(API_KEY);

        if ( apiKey == null || apiKey.equals("") ||apiKey.equals("{{api-key}}")) {
            SystemUser su = securityHelper.getSysUser(false);
            if ( su != null ) {
                if (su.getCompany() != null) {
                    request.setAttribute(COMPANY, su.getCompany());
                    request.setAttribute(API_KEY, su.getApiKey());
                    return true;
                }

            }
        } else {
        	logger.trace("Identifying company from api-key supplied in request HttpHeader" );
            Company company = securityHelper.getCompany(apiKey);
            if (company != null) {
                request.setAttribute(COMPANY, company);
                request.setAttribute(API_KEY, apiKey);
                return true;
            }
        }
        response.setContentType( MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        //response.sendError(HttpServletResponse.SC_FORBIDDEN, "This user account has no access to data");
        throw new SecurityException("Authentication is required to access this service");
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
    }

}
