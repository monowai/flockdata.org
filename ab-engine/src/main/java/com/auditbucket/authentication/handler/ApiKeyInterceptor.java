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
        	logger.trace("Identifying company from Api-Key supplied in request HttpHeader" );
            Company company = securityHelper.getCompany(apiKey);
            if (company != null) {
                request.setAttribute(COMPANY, company);
                return true;
            }
        }

        throw new SecurityException("You must be an authorized user to work with this service");
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
