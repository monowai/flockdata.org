/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.configure;

import org.flockdata.model.Company;
import org.flockdata.model.SystemUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler) throws Exception {

        if ( HttpMethod.OPTIONS == HttpMethod.resolve(request.getMethod()))
            return true; // CORS - this interceptor does not handle OPTIONS requests
        String apiKey = request.getHeader(API_KEY);

        if (noApiKey(apiKey)) {
            if (isDataAccessRequest(request)) { // Is it a data access endpoint?
                // Data access requests require a company
                // Resolve the user from the currently logged in user
                SystemUser su = securityHelper.getSysUser(false);
                if (su != null) {
                    if (su.getCompany() != null) {
                        request.setAttribute(COMPANY, su.getCompany());
                        request.setAttribute(API_KEY, su.getApiKey());
                        return true;
                    }
                } // Falls through to Forbidden

            } else {
                return true; // No APIKey, not a data access request; not our problem
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
        throw new SecurityException("A data access account is required to access this service. We couldn't resolve one for your login or apikey");
    }

    private boolean isDataAccessRequest(HttpServletRequest request) {
        String url = request.getRequestURL().toString();
        return ! (url.contains("/api/v1/admin/health") || url.contains("/api/v1/admin/ping"))
                && (url.contains("/api/v1/company")
                 || url.contains("/api/v1/track")
                 || url.contains("/api/v1/admin")
                || url.contains("/api/v1/concept")
                 || url.contains("/api/v1/entity")
                 || url.contains("/api/v1/fortress")
                 || url.contains("/api/v1/tag")
                 || url.contains("/api/v1/model")
                 || url.contains("/api/v1/path")
                 || url.contains("/api/v1/query")
                 || url.contains("/api/v1/doc")
                 || url.contains("/api/v1/geo"));
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
