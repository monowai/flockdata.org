/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

package org.flockdata.search;

import java.util.Objects;

/**
 * A single column that can be reported on
 * <p>
 * @author mholdsworth
 * @since 31/08/2016
 */
public class EsColumn {
    private String name;
    private String displayName;
    private String type;
    private String format;

    public EsColumn() {
    }

    public EsColumn(String name, String type) {
        this();
        this.name = name;
        this.type = type;
        this.displayName = name;
        // Compute a user friendly display name by removing general constants
        if (name.startsWith(SearchSchema.TAG_FIELD))
            displayName = name.substring(SearchSchema.TAG_FIELD.length());
        else if (name.startsWith(SearchSchema.ENTITY_FIELD))
            displayName = name.substring(SearchSchema.ENTITY_FIELD.length());
        else if (name.startsWith(SearchSchema.DATA_FIELD))
            displayName = name.substring(SearchSchema.DATA_FIELD.length());

        if ( name.endsWith(SearchSchema.FACET_FIELD))
            displayName = displayName.substring(0, displayName.length()-SearchSchema.FACET_FIELD.length());

        if ( name.equals(SearchSchema.DOC_TYPE))
            displayName = "type";

        if ( displayName.contains(SearchSchema.TAG_UDP))
            displayName = displayName.replace(SearchSchema.TAG_UDP,"");

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EsColumn)) return false;
        EsColumn esColumn = (EsColumn) o;
        return Objects.equals(name, esColumn.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "EsColumn{" +
                "name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                '}';
    }
}
