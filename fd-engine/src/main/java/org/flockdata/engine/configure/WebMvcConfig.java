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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Register any additional Interceptors for fd-engine
 * Created by mike on 18/02/16.
 */
@Configuration
@Controller
public class WebMvcConfig extends WebMvcConfigurerAdapter {

    @Autowired
    ApiKeyInterceptor apiKeyInterceptor;

    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor( apiKeyInterceptor);
    }

    @RequestMapping("/")
    String home() {
        return "index.html";
    }

    @RequestMapping("/api")
    String api () {
        return home();
    }

}
