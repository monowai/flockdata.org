package com.auditbucket.audit.bean;

import com.auditbucket.audit.model.IAuditHeader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Map;

/**
 * User: mike
 * Date: 22/06/13
 * Time: 6:28 AM
 */
public class SearchDocumentBean {
    private IAuditHeader auditHeader;
    private DateTime dateTime;
    private Map<String, Object> what;
    private String event;
    private static final ObjectMapper om = new ObjectMapper();

    public SearchDocumentBean(IAuditHeader auditHeader, DateTime dateTime, String what, String event) throws IOException {
        this.auditHeader = auditHeader;
        this.dateTime = dateTime;
        this.event = event;
        om.readValue(om.readTree(what).toString(), Map.class);
    }


    public IAuditHeader getAuditHeader() {
        return auditHeader;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public Map<String, Object> getWhat() {
        return what;
    }

    public String getEvent() {
        return event;
    }
}
