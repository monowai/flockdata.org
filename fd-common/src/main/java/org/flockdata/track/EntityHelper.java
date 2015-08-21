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

package org.flockdata.track;

/**
 * Created by mike on 5/07/15.
 */
public class EntityHelper {
    public static String parseKey(Long fortressId, Long documentId, String callerRef) {
        return  "" + fortressId + "." + documentId + "." + callerRef;
    }

    public static String getLabel(Iterable<String> labels) {
        for (String label : labels) {
            if (!isInternalLabel(label))
                return label;
        }
        return null;
    }

    public static boolean isInternalLabel(String label) {
        return label.equalsIgnoreCase("_Entity") || label.equalsIgnoreCase("Entity");
    }
}
