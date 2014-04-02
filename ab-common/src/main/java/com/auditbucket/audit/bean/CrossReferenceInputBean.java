package com.auditbucket.audit.bean;

import java.util.List;
import java.util.Map;

/**
 * Used to handle cross references from a source MetaHeader through to a collection of named references
 * User: mike
 * Date: 2/04/14
 * Time: 12:24 PM
 */
public class CrossReferenceInputBean {
    Map<String,List<String>>references;
    private String fortress;
    private String callerRef;

    protected CrossReferenceInputBean(){}

    public CrossReferenceInputBean(String fortress, String callerRef, Map<String,List<String>>references){
        this();
        this.callerRef = callerRef;
        this.fortress = fortress;
        this.references = references;
    }

    public String getCallerRef() {
        return callerRef;
    }

    public String getFortress() {
        return fortress;
    }

    public Map<String,List<String>>getReferences(){
        return references;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CrossReferenceInputBean)) return false;

        CrossReferenceInputBean that = (CrossReferenceInputBean) o;

        if (callerRef != null ? !callerRef.equals(that.callerRef) : that.callerRef != null) return false;
        if (fortress != null ? !fortress.equals(that.fortress) : that.fortress != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = fortress != null ? fortress.hashCode() : 0;
        result = 31 * result + (callerRef != null ? callerRef.hashCode() : 0);
        return result;
    }
}
