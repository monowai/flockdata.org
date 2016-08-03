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

package org.flockdata.profile;

import org.flockdata.transform.ColumnDefinition;

/**
 * Encapsulates the result of a single validation run
 * <p>
 * Created by mike on 14/04/16.
 */
public class ColumnValidationResult {
    private String sourceColumn;
    private ColumnDefinition column;
    private String message;

    public ColumnValidationResult() {

    }

    public ColumnValidationResult(String sourceColumn, ColumnDefinition column, String message) {
        this();
        this.sourceColumn = sourceColumn;
        this.column = column;
        this.message = message;
    }

    public ColumnDefinition getColumn() {
        return column;
    }


    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "ContentValidationResult{" +
                "sourceColumn=" + sourceColumn +
                '}';
    }
}
