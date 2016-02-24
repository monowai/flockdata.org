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

}