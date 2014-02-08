package com.auditbucket.helper;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.MappingJacksonJsonView;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: mike
 * Date: 31/01/14
 * Time: 6:27 PM
 * http://www.asyncdev.net/2011/12/spring-restful-controllers-and-error-handling/
 */
public class JsonError {
    private String message;

    public JsonError(String message) {
        this.message = message;
    }
    public ModelAndView asModelAndView() {
        MappingJacksonJsonView jsonView = new MappingJacksonJsonView();
        HashMap<String,Object> map = new HashMap<>();
        map.put("error", message);
        return new ModelAndView(jsonView, map );
    }
}
