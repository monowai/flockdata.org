/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
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
