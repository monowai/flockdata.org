package com.auditbucket.search.helper;

import com.auditbucket.helper.FlockException;
import com.auditbucket.helper.JsonMessage;
import com.fasterxml.jackson.core.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * Exception handler for Search service
 * User: mike
 * Date: 12/04/14
 * Time: 3:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class GlobalExceptionHandler {
    private Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(FlockException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleAuditException( FlockException ex){
        logger.error("Datagio Exception", ex);
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

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleInternal( Exception ex) {
        logger.error("Error 500", ex);
        return new JsonMessage(ex.getMessage()).asModelAndViewError();
    }
}
