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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * User: Mike Holdsworth
 * Since: 1/11/13
 */
public class AuditError {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String status;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String exceptionName;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> errors;

    protected AuditError() {
    }

    public AuditError(List<String> errors) {
        this.errors = errors;
    }

    public AuditError(String exceptionName, String message) {
        this(message);
        this.exceptionName = exceptionName;
    }

    public AuditError(String message) {
        this();
        this.message = message;
    }


    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public String getExceptionName() {
        return exceptionName;
    }

    public void setExceptionName(String exceptionName) {
        this.exceptionName = exceptionName;
    }

    public List<String> getErrors() {
        return errors;
    }
}
