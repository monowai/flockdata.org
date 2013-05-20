package com.auditbucket.audit.model;

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

    public abstract String getWho();

    public abstract Date getWhen();

    /**
     *
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
    public String getKey();

    public String getWhat();

    String getEvent();


    //public abstract ITag getTag();
}
