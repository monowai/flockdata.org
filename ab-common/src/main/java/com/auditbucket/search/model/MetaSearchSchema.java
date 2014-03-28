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

package com.auditbucket.search.model;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.model.Fortress;

/**
 * User: Mike Holdsworth
 * Since: 5/09/13
 */
public class MetaSearchSchema {
    // Storage schema used in a Search Document
    public static final String WHAT = "@what";
    public static final String WHEN = "@when";
    public static final String CALLER_REF = "@code";
    public static final String DESCRIPTION = "@description";
    public static final String TIMESTAMP = "@timestamp";
    public static final String FORTRESS = "@fortress";
    public static final String DOC_TYPE = "@docType";

    public static final String TAG = "@tag";
    public static final String LAST_EVENT = "@lastEvent";
    public static final String WHO = "@who";
    public static final String META_KEY = "@auditKey";
    public static String CREATED = "@whenCreated"; // Date the document was first created

    public static final String WHAT_CODE = "code";
    public static final String WHAT_NAME = "name";
    public static final String WHAT_DESCRIPTION = "description";

    // Analyzer NGram Config Settings
    public static final String NGRM_WHAT_DESCRIPTION = "ngram_what_description";
    public static final String NGRM_WHAT_CODE = "ngram_what_code";
    public static final String NGRM_WHAT_NAME = "ngram_what_name";

    public static final String NGRM_WHAT_DESCRIPTION_MIN = "3";
    public static final String NGRM_WHAT_DESCRIPTION_MAX = "10";

    public static final String NGRM_WHAT_CODE_MIN = "2";
    public static final String NGRM_WHAT_CODE_MAX = "5";

    public static final String NGRM_WHAT_NAME_MIN = "3";
    public static final String NGRM_WHAT_NAME_MAX = "10";

    public static String parseIndex(Fortress fortress) throws DatagioException {
        if (fortress.getCompany().getCode() == null)
            throw new DatagioException("Company code is null");
        return "ab." + fortress.getCompany().getCode() + "." + fortress.getCode();
    }

}
