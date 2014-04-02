package com.auditbucket.dao;

import com.auditbucket.audit.model.DocumentType;
import com.auditbucket.registration.model.Fortress;

/**
 * Created with IntelliJ IDEA.
 * User: mike
 * Date: 3/04/14
 * Time: 7:31 AM
 * To change this template use File | Settings | File Templates.
 */
public interface SchemaDao {
    DocumentType findDocumentType(Fortress company, String documentType, Boolean createIfMissing);


}
