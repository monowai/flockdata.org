package com.auditbucket.audit.repo.es.model;

import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.registration.model.IFortress;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import java.util.Date;

/**
 * User: mike
 * Date: 25/04/13
 * Time: 4:33 PM
 */
public class AuditChange implements IAuditChange {
    // ToDo: Figure out naming standard for system variables
    @Id
    private String id;
    private String headerKey;
    private String _dataType;
    private String what;
    private String name;
    private Date when;
    private String fortressName;
    private String companyName;
    private String event;
    private String _clientRef;
    @Version
    private Long version;

    private String indexName;

    /**
     * extracts relevant header records to be used in indexing
     *
     * @param header auditHeader details (owner of this change)
     */
    public AuditChange(IAuditHeader header) {
        this();
        setHeaderKey(header.getUID());
        this._dataType = header.getDataType();
        setFortress(header.getFortress());
        this.indexName = header.getIndexName();
        this._clientRef = header.getName();
    }

    public AuditChange() {
    }

    @Override
    @JsonIgnore
    public String getWhat() {
        return what;
    }

    @Override
    public void setWhat(String what) {
        this.what = what;
    }

    /**
     * @return Unique key in the index
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHeaderKey() {
        return headerKey;
    }

    public void setHeaderKey(String headerKey) {
        this.headerKey = headerKey;
    }

    private void setFortress(IFortress fortress) {
        this.setFortressName(fortress.getName());
        this.setCompanyName(fortress.getCompany().getName());

    }

    public String getName() {
        return name;
    }

    public void getName(String who) {
        this.name = who;
    }

    public Date getWhen() {
        return when;
    }

    @Override
    public void setVersion(long version) {
        this.version = version;
    }

    public void setWhen(Date when) {
        this.when = when;
    }

    public String getFortressName() {
        return fortressName;
    }

    @JsonIgnore
    public String getIndexName() {
        return indexName;
    }

    public void setFortressName(String fortressName) {
        this.fortressName = fortressName;
    }

    @JsonIgnore
    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Long getVersion() {
        return version;
    }

    public String getDataType() {
        return _dataType;
    }

    protected void setDataType(String dataType) {
        this._dataType = dataType;
    }
}
