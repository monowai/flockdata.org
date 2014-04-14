package com.auditbucket.dao;

import com.auditbucket.audit.model.DocumentType;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: mike
 * Date: 3/04/14
 * Time: 7:31 AM
 * To change this template use File | Settings | File Templates.
 */
public interface SchemaDao {
    DocumentType findDocumentType(Fortress company, String documentType, Boolean createIfMissing);

    Collection<DocumentType> getFortressDocumentsInUse(Fortress fortress);

    public Collection<DocumentType> getCompanyDocumentsInUse(Company company);

    public boolean registerTagIndex (Company c, String indexName );

    public void ensureUniqueIndexes(Company c, Iterable<TagInputBean> tagInputs, Collection<String> existingIndexes );

    void ensureSystemIndexes(Company company, String tagSuffix);
}
