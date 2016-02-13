package org.flockdata.search.integration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mike on 13/02/16.
 */
@Configuration
@EnableWebMvc
@Profile({"integration","production"})
public class ConfigureMvc extends WebMvcConfigurerAdapter {

    @Override
    public void configureContentNegotiation(
            ContentNegotiationConfigurer configurer) {
        final Map<String, String> parameterMap = new HashMap<>();
        parameterMap.put("charset", "utf-8");

        configurer.defaultContentType(new MediaType(
                MediaType.APPLICATION_JSON, parameterMap));
    }
}