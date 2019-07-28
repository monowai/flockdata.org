/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.search.helper;

import com.fasterxml.jackson.core.JsonParseException;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * Exception handler for Search service
 *
 * @author mholdsworth
 * @since 12/04/2014
 */
public class GlobalExceptionHandler {
  private Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(FlockException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ModelAndView handleAuditException(FlockException ex) {
    logger.error("Datagio Exception", ex);
    return new JsonMessage(ex.getMessage()).asModelAndViewError();
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ModelAndView handleIAException(IllegalArgumentException ex) {
    return new JsonMessage(ex.getMessage()).asModelAndViewError();
  }

  @ExceptionHandler(JsonParseException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ModelAndView handleJsonError(final JsonParseException ex) {
    return new JsonMessage(ex.getMessage()).asModelAndViewError();
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ModelAndView handleInternal(Exception ex) {
    logger.error("Error 500", ex);
    return new JsonMessage(ex.getMessage()).asModelAndViewError();
  }
}
