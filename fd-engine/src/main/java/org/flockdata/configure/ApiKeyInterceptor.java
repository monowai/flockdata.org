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

package org.flockdata.configure;

import org.flockdata.model.Company;
import org.flockdata.model.SystemUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
public class ApiKeyInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory
            .getLogger(ApiKeyInterceptor.class);
    public static final String COMPANY = "company";
    public static final String API_KEY = "api-key";

    @Autowired
    private SecurityHelper securityHelper;

//    @Autowired
//    @Qualifier("engineConfig")
//    PlatformConfig engineConfig;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler) throws Exception {
        String apiKey = request.getHeader(API_KEY);

        if (noApiKey(apiKey)) {
            // This has nothing to do with us
            if (isCompanyLookupUrl(request)) {
                // Resolve the user from the currently logged in user
                SystemUser su = securityHelper.getSysUser(false);
                if (su != null) {
                    if (su.getCompany() != null) {
                        request.setAttribute(COMPANY, su.getCompany());
                        request.setAttribute(API_KEY, su.getApiKey());
                        return true;
                    }
                } // Fall through to Forbidden

            } else {
                return true; // No APIKey, no data access request; not our problem
            }
        } else {
            // Attempting to authenticate via the api secret
            logger.trace("Identifying company from api-key supplied in request HttpHeader");
            Company company = securityHelper.getCompany(apiKey);
            if (company != null) {
                request.setAttribute(COMPANY, company);
                request.setAttribute(API_KEY, apiKey);
                return true;
            }
        }
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        //response.sendError(HttpServletResponse.SC_FORBIDDEN, "This user account has no access to data");
        throw new SecurityException("Authentication is required to access this service");
        //return false;
    }

    private boolean isCompanyLookupUrl(HttpServletRequest request) {
        String url = request.getRequestURL().toString();
        return url.contains("/api/v1/company")
                || url.contains("/api/v1/track")
                || url.contains("/api/v1/entity")
                || url.contains("/api/v1/fortress")
                || url.contains("/api/v1/tag")
                || url.contains("/api/v1/doc")
                || url.contains("/api/v1/geo")
                ;
    }

    private boolean noApiKey(String apiKey) {
        return (apiKey == null || apiKey.equals("") || apiKey.equals("{{api-key}}"));
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

    /**
     * API key precedence
     * <p>
     * User: mike
     * Date: 15/03/14
     * Time: 11:51 AM
     */
    public static class ApiKeyHelper {
        /**
         * api key in the HttpHeader overrides one in the request
         *
         * @param headerKey  headerKey
         * @param requestKey requestKey
         * @return null or param.
         */
        public static String resolveKey(String headerKey, String requestKey) {
            String key = requestKey;
            if (headerKey != null && (headerKey.startsWith("{{") && headerKey.endsWith("}}"))) // Postman "not-set" value
                headerKey = null;
            if (headerKey != null)
                key = headerKey;
            return key;
        }
    }
}
