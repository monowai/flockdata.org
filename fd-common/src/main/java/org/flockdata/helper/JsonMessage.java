/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.helper;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: mike
 * Date: 31/01/14
 * Time: 6:27 PM
 * http://www.asyncdev.net/2011/12/spring-restful-controllers-and-error-handling/
 */
public class JsonMessage {
    private String message;

    public JsonMessage(String message) {
        this.message = message;
    }
    public ModelAndView asModelAndViewError() {
        MappingJackson2JsonView jsonView = new MappingJackson2JsonView();
        HashMap<String,Object> map = new HashMap<>();
        map.put("message", message);
        return new ModelAndView(jsonView, map );
    }
    public ModelAndView asModelAndViewMessage() {
        MappingJackson2JsonView jsonView = new MappingJackson2JsonView();
        HashMap<String,Object> map = new HashMap<>();
        map.put("message", message);
        return new ModelAndView(jsonView, map );
    }

}
