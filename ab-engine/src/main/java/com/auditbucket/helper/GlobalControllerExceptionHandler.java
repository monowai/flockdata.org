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

import com.fasterxml.jackson.core.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.keyvalue.riak.DataStoreOperationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * User: Mike Holdsworth
 * Date: 15/06/13
 * Time: 12:38 PM
 *
 * http://www.asyncdev.net/2011/12/spring-restful-controllers-and-error-handling/
 *
 */
@ControllerAdvice
public class GlobalControllerExceptionHandler {
    private Logger logger = LoggerFactory.getLogger(GlobalControllerExceptionHandler.class);

    @ExceptionHandler(DataStoreOperationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleKVStore( DataStoreOperationException ex){
        logger.error("KV Store Error", ex);
        return new JsonError("Internal KV Error. Contact Support").asModelAndView();
    }

    @ExceptionHandler(DatagioException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleAuditException( DatagioException ex){
        logger.error("Datagio Exception", ex);
        return new JsonError(ex.getMessage()).asModelAndView();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleIAException( IllegalArgumentException ex){
        return new JsonError(ex.getMessage()).asModelAndView();
    }

    @ExceptionHandler(JsonParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleJsonError(final JsonParseException ex) {
        return new JsonError(ex.getMessage()).asModelAndView();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleInternal( Exception ex) {
        logger.error("Error 500", ex);
        return new JsonError(ex.getMessage()).asModelAndView();
    }

}
