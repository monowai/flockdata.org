package org.flockdata.track.bean;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Collection;

/**
 * Created by mike on 20/07/15.
 */
public class TrackRequestResult implements Serializable{

    String company;
    String fortressCode;
    String callerRef;
    String metaKey;

    boolean newEntity;
    ContentInputBean.LogStatus logStatus;

    Collection<String> serviceMessages = null;

    TrackRequestResult() {}

    public TrackRequestResult(TrackResultBean resultBean){
        this();
        this.metaKey = resultBean.getMetaKey();
        this.callerRef = resultBean.getEntity().getCode();
        this.fortressCode = resultBean.getEntity().getSegment().getCode();
        logStatus = resultBean.getLogStatus();
        serviceMessages  = resultBean.getServiceMessages();
        boolean newEntity = resultBean.isNewEntity();
    }

    public String getMetaKey() {
        return metaKey;
    }

    public String getCallerRef() {
        return callerRef;
    }

    public String getFortressCode() {
        return fortressCode;
    }

    public String getCompany() {
        return company;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public ContentInputBean.LogStatus getLogStatus() {
        return logStatus;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Collection<String> getServiceMessages() {
        return serviceMessages;
    }

    public boolean isNewEntity() {
        return newEntity;
    }


}
