package com.auditbucket.audit.model;

import com.auditbucket.registration.model.ITag;

/**
 * User: mike
 * Date: 26/06/13
 * Time: 12:52 PM
 */
public interface ITagValue {

    public ITag getTag();

    //ToDo - should this be a taggable interface?
    public IAuditHeader getHeader();

    public String getTagValue();

}
