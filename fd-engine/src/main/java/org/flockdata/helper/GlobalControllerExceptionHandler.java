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

import com.fasterxml.jackson.core.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
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

    @ExceptionHandler(FlockException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleAppException(FlockException ex){
        logger.error("Processing Exception- {}", ex.getLocalizedMessage());
        return new JsonMessage(ex.getMessage()).asModelAndViewError();
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNotFound(NotFoundException ex){
        logger.error("Resource Not Found Exception- {}", ex.getLocalizedMessage());
        return new JsonMessage(ex.getMessage()).asModelAndViewError();
    }


    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleIAException( IllegalArgumentException ex){
        return new JsonMessage(ex.getMessage()).asModelAndViewError();
    }

    @ExceptionHandler(JsonParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleJsonError(final JsonParseException ex) {
        return new JsonMessage(ex.getMessage()).asModelAndViewError();
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ModelAndView handleSecException(final SecurityException ex){
        return new JsonMessage(ex.getMessage()).asModelAndViewError();
    }
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ModelAndView handleSecAuthException(final AuthenticationException ex){
        return new JsonMessage(ex.getMessage()).asModelAndViewError();
    }


    //.class
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ModelAndView handleAuthException(final AccessDeniedException ex){
        return new JsonMessage(ex.getMessage()).asModelAndViewError();
    }

    @ExceptionHandler(HttpMessageConversionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleConversionProblem( final HttpMessageConversionException ex){
        return new JsonMessage(ex.getMessage()).asModelAndViewError();
    }

    @ExceptionHandler(Exception.class )
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleInternal( Exception ex) {
        logger.error("Error 500", ex);
        if ( ex.getCause() !=null )
            return new JsonMessage(ex.getCause().getMessage()).asModelAndViewError();
        else
            return new JsonMessage(ex.getMessage()).asModelAndViewError();
    }

}
