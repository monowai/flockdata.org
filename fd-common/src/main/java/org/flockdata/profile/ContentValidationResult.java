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
 *
 * Created by mike on 14/04/16.
 */
public class ContentValidationResult {
    ColumnDefinition column;
    String status ;

    public ContentValidationResult(){

    }
    public ContentValidationResult(ColumnDefinition column, String status) {
        this();
        this.column = column;
        this.status = status;
    }

    public ColumnDefinition getColumn() {
        return column;
    }
}
