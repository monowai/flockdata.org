/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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

package com.auditbucket.bean;

import javax.validation.constraints.NotNull;

/**
 * User: Mike Holdsworth
 * Date: 28/06/13
 * Time: 9:58 PM
 */
public class AuditTagInputBean {
    @NotNull
    private String tagName;
    @NotNull
    private String auditKey;
    @NotNull
    private String type;

    protected AuditTagInputBean() {
    }

    public AuditTagInputBean(String auditKey, String tagName, String type) {
        this.auditKey = auditKey;
        this.tagName = tagName;
        // Stragetically this should be a named relationship
        if (type == null)
            this.type = "general";
        else
            this.type = type;
    }

    public String getAuditKey() {
        return auditKey;
    }

    public String getTagName() {
        return tagName;
    }

    public String getType() {
        return type;
    }
}
