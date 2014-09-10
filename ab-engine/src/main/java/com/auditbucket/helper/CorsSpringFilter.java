package com.auditbucket.helper;

import com.thetransactioncompany.cors.CORSConfiguration;
import com.thetransactioncompany.cors.CORSFilter;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import java.io.IOException;

/**
 * Wraps the implementing CORS filter so that we can initialise it from a Spring context
 * <p/>
 * User: mike
 * Date: 9/09/14
 * Time: 8:46 AM
 */
@Component
public class CorsSpringFilter implements javax.servlet.Filter {
    static CORSFilter corsFilter;

    public CorsSpringFilter(CORSConfiguration corsConfiguration) throws ServletException {
        corsFilter = new CORSFilter(corsConfiguration);
    }

    public CorsSpringFilter() {
        super();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        corsFilter.doFilter(request, response, chain);
    }

    @Override
    public void destroy() {
        corsFilter.destroy();
    }
}
