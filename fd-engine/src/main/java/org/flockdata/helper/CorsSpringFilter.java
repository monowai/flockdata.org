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
