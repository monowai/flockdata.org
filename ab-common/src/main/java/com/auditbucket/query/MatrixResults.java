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

package com.auditbucket.query;

import java.util.Collection;

/**
 * User: mike
 * Date: 12/06/14
 * Time: 2:17 PM
 */
public class MatrixResults {
    Collection<MatrixResult>results;

    @Override
    public String toString() {
        return "MatrixResults{" +
                "results=" + results.size() +
                '}';
    }

    public MatrixResults(Collection<MatrixResult> matrixResults) {
        setResults(matrixResults);
    }

    public Collection<MatrixResult> getResults() {
        return results;
    }

    public void setResults(Collection<MatrixResult> results) {
        this.results = results;
    }
}
