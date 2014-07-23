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

package com.auditbucket.track.model;

/**
 * User: mike
 * Date: 9/07/14
 * Time: 5:01 PM
 */
public class MetaKey {
    private String fortressName;
    private String documentType;
    private String callerRef;


    public MetaKey(){}

    public MetaKey (String fortressName, String documentType, String callerRef){
        this.fortressName = fortressName;
        this.documentType = documentType;
        this.callerRef = callerRef;
    }

    public MetaKey(String callerRef) {
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
        if (!(o instanceof MetaKey)) return false;

        MetaKey metaKey = (MetaKey) o;

        if (!callerRef.equals(metaKey.callerRef)) return false;
        if (documentType != null ? !documentType.equals(metaKey.documentType) : metaKey.documentType != null)
            return false;
        if (fortressName != null ? !fortressName.equals(metaKey.fortressName) : metaKey.fortressName != null)
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
        return "MetaKey{" +
                "fortressName='" + fortressName + '\'' +
                ", documentType='" + documentType + '\'' +
                ", callerRef='" + callerRef + '\'' +
                '}';
    }
}
