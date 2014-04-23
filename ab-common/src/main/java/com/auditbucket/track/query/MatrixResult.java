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

package com.auditbucket.track.query;

/**
 * Pojo for carrying matrix result data
 * User: mike
 * Date: 19/04/14
 * Time: 6:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class MatrixResult {
    private String conceptFrom;
    private String conceptTo;
    private Long occurrenceCount;

    public MatrixResult(String conceptFrom, String conceptTo, Long occurrenceCount) {
        this();
        this.conceptFrom = conceptFrom;
        this.conceptTo = conceptTo;
        this.occurrenceCount = occurrenceCount;

    }

    public MatrixResult() {
    }

    public String getConceptFrom() {
        return conceptFrom;
    }

    public void setConceptFrom(String conceptFrom) {
        this.conceptFrom = conceptFrom;
    }

    public String getConceptTo() {
        return conceptTo;
    }

    public void setConceptTo(String conceptTo) {
        this.conceptTo = conceptTo;
    }

    public Long getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(Long occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }
}
