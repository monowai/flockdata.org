/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

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
public class JsonMessage {
    private String message;

    public JsonMessage(String message) {
        this.message = message;
    }
    public ModelAndView asModelAndViewError() {
        MappingJacksonJsonView jsonView = new MappingJacksonJsonView();
        HashMap<String,Object> map = new HashMap<>();
        map.put("error", message);
        return new ModelAndView(jsonView, map );
    }
    public ModelAndView asModelAndViewMessage() {
        MappingJacksonJsonView jsonView = new MappingJacksonJsonView();
        HashMap<String,Object> map = new HashMap<>();
        map.put("message", message);
        return new ModelAndView(jsonView, map );
    }

}
