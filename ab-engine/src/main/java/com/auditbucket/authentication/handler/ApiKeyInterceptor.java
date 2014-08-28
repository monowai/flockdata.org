package com.auditbucket.authentication.handler;

import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.SystemUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ApiKeyInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory
            .getLogger(ApiKeyInterceptor.class);
    public static final String COMPANY = "company";
    public static final String API_KEY = "Api-Key";

    @Autowired
    private SecurityHelper securityHelper;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler) throws Exception {
        String apiKey = request.getHeader(API_KEY);

        if ( apiKey == null ) {
            SystemUser su = securityHelper.getSysUser(false);
            if ( su != null ) {
                apiKey = su.getApiKey();
                if (su.getCompany() != null) {
                    request.setAttribute("company", su.getCompany());
                    return true;
                }

            }
        }

        if (apiKey != null) {
            Company company = securityHelper.getCompany(apiKey);
            if (company != null) {
                request.setAttribute("company", company);
                return true;
            }
        }
        // Not necessarily forbidden, just no API key to work with data.
        // Admin users can create data access users but not access data themselves.
        // The 3 scenarios are:
        //      Authorised with no api key (no company object returned)
        //      Authorised with an api key (company object returned)
        //      Not authorised             (forbidden)
        throw new SecurityException("Unable to resolve or verify your API Key");
        //response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unauthorized");
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
