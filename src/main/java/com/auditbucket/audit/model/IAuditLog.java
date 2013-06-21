package com.auditbucket.audit.model;

import com.auditbucket.registration.model.IFortressUser;

import java.util.Date;

/**
 * User: mike
 * Date: 15/04/13
 * Time: 5:49 AM
 */
public interface IAuditLog {

    String CREATE = "Create";
    String UPDATE = "Update";

    public abstract IAuditHeader getHeader();

    public abstract IFortressUser getWho();

    public abstract Date getWhen();

    /**
     * @return UTC time that this record was created
     */
    public abstract Date getSysWhen();

    public String getComment();

    /**
     * optional comment
     *
     * @param comment searchable.
     */
    public void setComment(String comment);

    public void setKey(String changeKey);

    /**
     * @return unique identifier to the search index key
     */
    public String getSearchKey();

    public String getWhat();

    String getEvent();

    public void setTxRef(ITxRef txRef);

    //public abstract ITag getTag();
}
