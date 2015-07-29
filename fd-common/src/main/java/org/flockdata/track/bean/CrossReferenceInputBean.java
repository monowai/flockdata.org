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

package org.flockdata.track.bean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Used to handle cross references from a source Entity through to a collection of named references
 * User: mike
 * Date: 2/04/14
 * Time: 12:24 PM
 */
public class CrossReferenceInputBean {
    Map<String,List<EntityKeyBean>>references;
    private String fortress;
    private String documentType;
    private String callerRef;
    private String serviceMessage;
    Map<String,List<EntityKeyBean>>ignored;

    protected CrossReferenceInputBean(){}

//    public CrossReferenceInputBean(String fortress, String callerRef, Map<String,List<EntityKey>>references){
//        this();
//        this.callerRef = callerRef;
//        this.fortress = fortress;
//        this.references = references;
//    }
//
//    public CrossReferenceInputBean(String sourceFortress, String sourceDocumentType, String sourceCallerRef, Map<String,List<EntityKey>>references){
//        this(sourceFortress, sourceCallerRef, references);
//        if ( sourceDocumentType!=null && !sourceDocumentType.equals("*"))
//            this.documentType = sourceDocumentType;
//    }

    public CrossReferenceInputBean(EntityInputBean entityInputBean) {
        this.callerRef = entityInputBean.getCode();
        this.fortress = entityInputBean.getFortress();
        this.documentType = entityInputBean.getDocumentName();
        this.references = entityInputBean.getCrossReferences();
    }

    public String getCallerRef() {
        return callerRef;
    }

    public String getFortress() {
        return fortress;
    }

    public Map<String,List<EntityKeyBean>>getReferences(){
        return references;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CrossReferenceInputBean)) return false;

        CrossReferenceInputBean that = (CrossReferenceInputBean) o;

        if (callerRef != null ? !callerRef.equals(that.callerRef) : that.callerRef != null) return false;
        if (documentType != null ? !documentType.equals(that.documentType) : that.documentType != null) return false;
        return !(fortress != null ? !fortress.equals(that.fortress) : that.fortress != null);

    }

    @Override
    public int hashCode() {
        int result = fortress != null ? fortress.hashCode() : 0;
        result = 31 * result + (callerRef != null ? callerRef.hashCode() : 0);
        result = 31 * result + (documentType != null ? documentType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CrossReferenceInputBean{" +
                "callerRef='" + callerRef + '\'' +
                ", references=" + references.size() +
                ", fortress='" + fortress + '\'' +
                ", docType ='" + documentType + '\'' +
                '}';
    }

    public void setServiceMessage(String serviceMessage) {
        this.serviceMessage = serviceMessage;
    }

    public String getServiceMessage() {
        return serviceMessage;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setIgnored(String xRefName, List<EntityKeyBean> ignored) {
        if (this.ignored == null )
           this.ignored = new HashMap<>();
        this.ignored.put(xRefName, ignored);
    }

    public Map<String,List<EntityKeyBean>> getIgnored() {
        return ignored;
    }
}
