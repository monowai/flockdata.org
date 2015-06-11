/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

package org.flockdata.search.model;

import org.flockdata.helper.FlockException;
import org.flockdata.registration.model.Fortress;

/**
 * User: Mike Holdsworth
 * Since: 5/09/13
 */
public class EntitySearchSchema {
    // Storage schema used in a Search Document
    public static final String WHAT = "what";
    public static final String CALLER_REF = "code";
    public static final String DESCRIPTION = "description";
    public static final String TIMESTAMP = "timestamp";
    public static final String FORTRESS = "fortress";
    public static final String DOC_TYPE = "docType";
    public static final String ATTACHMENT = "attachment";

    public static final String TAG = "tag";
    public static final String ALL_TAGS = "tags";
    public static final String LAST_EVENT = "lastEvent";
    public static final String WHO = "who";
    public static final String META_KEY = "metaKey";
    public static final String CREATED = "whenCreated"; // Date the document was first created in the Fortress
    public static final String UPDATED = "whenUpdated";

    public static final String WHAT_CODE = "code";
    public static final String WHAT_NAME = "name";
    public static final String WHAT_DESCRIPTION = "description";
    public static final String FILENAME = "filename";
    public static final String CONTENT_TYPE = "contentType";
    public static final String PREFIX = "fd.";
    public static final String PROPS = "udp";

    public static String parseIndex(Fortress fortress) {

        return parseIndex(fortress.getCompany().getCode()) + fortress.getCode();
    }

    private static String parseIndex(String companyCode) {
        // ToDo: Add multi tenant test. Company must always be present if MultiTenanted
        if (companyCode == null || companyCode.equals(""))
            companyCode = "*";

        return PREFIX + companyCode.toLowerCase() + ".";
    }

    public static String parseIndex(String company, String fortress) {
        return PREFIX + company.toLowerCase() + "." + fortress.toLowerCase();
    }

    public static String parseIndex(QueryParams queryParams) throws FlockException {
        String prefix = parseIndex(queryParams.getCompany());
        if (queryParams.getFortress() == null || queryParams.getFortress().equals(""))
            return prefix + "*";


        return prefix + queryParams.getFortress().toLowerCase();
    }

}
