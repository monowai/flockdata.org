package com.auditbucket.audit.model;

import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.IFortressUser;

import java.util.Set;

public interface IAuditHeader {

    public abstract Long getId();

    public abstract IFortress getFortress();

    public abstract String getDataType();

    /**
     * @return Global Unique ID
     */
    public abstract String getUID();

    /**
     * @return last fortress user to modify this record
     */
    public IFortressUser getLastUser();

    public void setLastUser(IFortressUser user);

    /**
     * @return fortress user who create the record
     */
    public IFortressUser getCreatedBy();

    /**
     * Who created this record
     *
     * @param user Fortress User who created this record
     */
    public void setCreatedUser(IFortressUser user);

    /**
     * @return the index name to use for subsequent changes
     */
    public String getIndexName();

    /**
     * @return unique identify the fortress recognises for the recordType.
     */
    public String getName();

    /**
     * alters the lastChange value
     */
    void bumpUpdate();

    Set<IAuditLog> getAuditLogs();

    Set<ITagRef> getTags();

    void setSearchKey(String parentKey);
}