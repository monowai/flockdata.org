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

package org.flockdata.data;


/**
 * FD organises system data into sub-documents
 * <p>
 * Each Entity is classified as being of a "DocumentType"
 * <p>
 * For example, Invoice, Customer, Person etc.
 * <p>
 * @author mholdsworth
 * @since 30/06/2013
 * @tag DocumentType
 */

public interface Document {
    String getName();

    String getGeoQuery();

    Boolean isSearchEnabled();

    Boolean isStoreEnabled();

    Boolean isTrackEnabled();

    EntityTag.TAG_STRUCTURE getTagStructure();

    Document.VERSION getVersionStrategy();

    String getCode();

    Long getId();

    Fortress getFortress();

    /**
     * Set the version strategy on a per DocumentType basis
     * <p>
     * Enable version control when segment.storeEnabled== false
     * Suppress when your segment.storeEnabled== true and you don't want to version
     * Fortress (default) means use whatever the segment default is
     */
    enum VERSION {
        FORTRESS, ENABLE, DISABLE
    }
}
