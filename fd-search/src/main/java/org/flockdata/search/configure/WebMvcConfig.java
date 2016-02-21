package org.flockdata.search.configure;

import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Spring MVC configuration
 * Created by mike on 18/02/16.
 */
@Configuration
@Controller
public class WebMvcConfig extends WebMvcConfigurerAdapter {


    @RequestMapping("/")
    String home() {
        return "index.html";
    }

}
