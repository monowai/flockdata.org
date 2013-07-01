package com.auditbucket.audit.repo.es.model;

import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.registration.model.IFortress;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import java.util.Date;
import java.util.Map;

/**
 * User: mike
 * Date: 25/04/13
 * Time: 4:33 PM
 */
public class AuditChange implements IAuditChange {
    // ToDo: Figure out naming standard for system variables
    @Id
    private String id;
    private String documentType;
    private Map<String, Object> what;
    private Date when;
    private String fortressName;
    private String companyName;
    private String name;
    private String who;
    private String auditKey;
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
        setName(header.getAuditKey());
        this.auditKey = header.getAuditKey();
        this.documentType = header.getDocumentType();
        setFortress(header.getFortress());
        this.indexName = header.getIndexName();
        this.searchKey = header.getSearchKey();
    }

    public AuditChange() {
    }

    public AuditChange(IAuditHeader header, String event, Map<String, Object> what) {
        this(header);
        this.name = event;
        this.what = what;
    }

    @Override
    @JsonIgnore
    public Map<String, Object> getWhat() {
        return what;
    }

    @Override
    public void setWhat(Map<String, Object> what) {
        this.what = what;
    }

    /**
     * @return Unique key in the index
     */
    @JsonIgnore
    public String getId() {
        return id;
    }

    private String searchKey;

    @JsonIgnore
    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    public String getSearchKey() {
        return searchKey;
    }


    public void setId(String id) {
        this.id = id;
    }


    private void setFortress(IFortress fortress) {
        this.setFortressName(fortress.getName());
        this.setCompanyName(fortress.getCompany().getName());

    }

    public String getName() {
        return name;
    }

    @Override
    public String getWho() {
        return this.who;
    }

    public void setName(String who) {
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

    @Override
    public void setWho(String name) {
        this.who = name;
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

    @JsonIgnore
    public Long getVersion() {
        return version;
    }

    public String getDocumentType() {
        return documentType;
    }

    protected void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getAuditKey() {
        return auditKey;
    }

    @JsonIgnore
    public String getRoutingKey() {
        return getAuditKey();
    }
}
