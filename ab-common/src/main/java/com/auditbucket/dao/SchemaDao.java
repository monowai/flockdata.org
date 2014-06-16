package com.auditbucket.dao;

import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.track.model.Concept;
import com.auditbucket.track.model.DocumentType;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Neo and Engine Schema related IO
 *
 * User: mike
 * Date: 3/04/14
 * Time: 7:31 AM
 */
public interface SchemaDao {
    DocumentType findDocumentType(Fortress company, String documentType, Boolean createIfMissing);

    Collection<DocumentType> getFortressDocumentsInUse(Fortress fortress);

    public Collection<DocumentType> getCompanyDocumentsInUse(Company company);

    public boolean registerTagIndex (Company c, String indexName );

    public void ensureUniqueIndexes(Company c, Iterable<TagInputBean> tagInputs, Collection<String> existingIndexes );

    void ensureSystemIndexes(Company company, String tagSuffix);

    void registerConcepts(Company company, Map<DocumentType, Collection<TagInputBean>> concepts);

    Set<Concept> findConcepts(Company company, Collection<String> documents, boolean withRelationships);
}
