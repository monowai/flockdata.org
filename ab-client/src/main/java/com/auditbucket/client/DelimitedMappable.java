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

package com.auditbucket.client;

/**
 * User: Mike Holdsworth
 * Since: 25/01/14
 */

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Support class to handle mapping from one format to another format
 * User: Mike Holdsworth
 * Since: 13/10/13
 */
public interface DelimitedMappable extends Mappable {

    String setData(String[] headerRow, String[] line, StaticDataResolver staticDataResolver) throws JsonProcessingException;

    @JsonIgnore
    boolean hasHeader();

    DelimitedMappable newInstance(boolean simulateOnly);

    @JsonIgnore
    char getDelimiter();


}
