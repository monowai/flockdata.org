package com.auditbucket.track.bean;

import com.auditbucket.track.model.EntityKey;

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
    Map<String,List<EntityKey>>references;
    private String fortress;
    private String documentType;
    private String callerRef;
    private String serviceMessage;
    Map<String,List<EntityKey>>ignored;

    protected CrossReferenceInputBean(){}

    public CrossReferenceInputBean(String fortress, String callerRef, Map<String,List<EntityKey>>references){
        this();
        this.callerRef = callerRef;
        this.fortress = fortress;
        this.references = references;
    }

    public CrossReferenceInputBean(String sourceFortress, String sourceDocumentType, String sourceCallerRef, Map<String,List<EntityKey>>references){
        this(sourceFortress, sourceCallerRef, references);
        if ( sourceDocumentType!=null && !sourceDocumentType.equals("*"))
            this.documentType = sourceDocumentType;
    }

    public String getCallerRef() {
        return callerRef;
    }

    public String getFortress() {
        return fortress;
    }

    public Map<String,List<EntityKey>>getReferences(){
        return references;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CrossReferenceInputBean)) return false;

        CrossReferenceInputBean that = (CrossReferenceInputBean) o;

        if (callerRef != null ? !callerRef.equals(that.callerRef) : that.callerRef != null) return false;
        if (documentType != null ? !documentType.equals(that.documentType) : that.documentType != null) return false;
        if (fortress != null ? !fortress.equals(that.fortress) : that.fortress != null) return false;

        return true;
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

    public void setIgnored(String xRefName, List<EntityKey> ignored) {
        if (this.ignored == null )
           this.ignored = new HashMap<>();
        this.ignored.put(xRefName, ignored);
    }

    public Map<String,List<EntityKey>> getIgnored() {
        return ignored;
    }
}
