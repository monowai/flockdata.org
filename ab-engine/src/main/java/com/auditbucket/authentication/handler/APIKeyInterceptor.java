package com.auditbucket.authentication.handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.model.Company;

public class APIKeyInterceptor implements HandlerInterceptor {
	private static final Logger logger = LoggerFactory
			.getLogger(APIKeyInterceptor.class);

	@Autowired
	private SecurityHelper securityHelper;

	@Override
	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler) throws Exception {
		String apiKey = request.getParameter("apiKey");
		logger.info("Api Key - " + apiKey);

		if (apiKey != null) {
			Company company = securityHelper.getCompany(apiKey);
			if (company != null) {
				request.setAttribute("company", company.getName());
				return true;
			} else {
				return false;
			}
		}
		return false;
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
