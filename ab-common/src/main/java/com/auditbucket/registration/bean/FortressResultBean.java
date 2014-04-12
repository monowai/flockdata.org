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

package com.auditbucket.registration.bean;

import com.auditbucket.registration.model.Fortress;

/**
 * User: Mike Holdsworth
 * Since: 21/12/13
 */
public class FortressResultBean {
    private String name;
    private String fortressKey;
    private String timeZone;

    protected FortressResultBean() {

    }

    public FortressResultBean(Fortress fortress) {
        this();
        this.name = fortress.getName();
        this.fortressKey = fortress.getFortressKey();
        this.timeZone = fortress.getTimeZone();
    }

    public String getName() {
        return name;
    }

    public String getFortressKey() {
        return fortressKey;
    }

    public String getTimeZone() {
        return timeZone;
    }
}
