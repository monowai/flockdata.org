package com.auditbucket.helper;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * User: mike
 * Date: 12/06/13
 * Time: 12:38 PM
 */
@ControllerAdvice
public class GlobalControllerExceptionHandler {

    @ExceptionHandler(value = {MissingServletRequestParameterException.class, HttpMessageNotReadableException.class, Exception.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ResponseEntity<Object> handleConflict(Exception ex) {
        return new ResponseEntity<Object>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

}
