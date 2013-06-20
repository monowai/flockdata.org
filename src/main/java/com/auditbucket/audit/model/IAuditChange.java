package com.auditbucket.audit.model;

import java.util.Date;

/**
 * User: mike
 * Date: 21/04/13
 * Time: 7:44 PM
 */
public interface IAuditChange {

    public void setWhat(String what);

    public String getWhat();

    public String getName();

    public String getWho();

    public String getFortressName();

    public String getCompanyName();

    public String getIndexName();

    public Date getWhen();

    public void setId(String id);

    public void setVersion(long version);

    public Long getVersion();

    public String getRecordType();

    public String getId();

    public void setSearchKey(String parent);

    public String getSearchKey();

    void setEvent(String event);

    void setWhen(Date date);

    void setWho(String name);
}
