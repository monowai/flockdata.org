/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.track.model;

/**
 * User: mike
 * Date: 9/07/14
 * Time: 5:01 PM
 */
public class EntityKey {
    private String fortressName;
    private String documentType;
    private String callerRef;


    public EntityKey(){}

    public EntityKey(String fortressName, String documentType, String callerRef){
        this.fortressName = fortressName;
        this.documentType = documentType;
        this.callerRef = callerRef;
    }

    public EntityKey(String callerRef) {
        this.callerRef = callerRef;
    }

    public String getFortressName() {
        return fortressName;
    }

    public String getDocumentType() {
        if ( documentType == null || documentType.equals(""))
            return "*";
        return documentType;
    }

    public String getCallerRef() {
        return callerRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityKey)) return false;

        EntityKey entityKey = (EntityKey) o;

        if (!callerRef.equals(entityKey.callerRef)) return false;
        if (documentType != null ? !documentType.equals(entityKey.documentType) : entityKey.documentType != null)
            return false;
        if (fortressName != null ? !fortressName.equals(entityKey.fortressName) : entityKey.fortressName != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = fortressName != null ? fortressName.hashCode() : 0;
        result = 31 * result + (documentType != null ? documentType.hashCode() : 0);
        result = 31 * result + callerRef.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "EntityKey {" +
                "fortressName='" + fortressName + '\'' +
                ", documentType='" + documentType + '\'' +
                ", callerRef='" + callerRef + '\'' +
                '}';
    }
}
