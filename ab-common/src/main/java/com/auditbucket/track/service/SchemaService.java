package com.auditbucket.track.service;

import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.track.bean.DocumentResultBean;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.DocumentType;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * User: mike
 * Date: 5/09/14
 * Time: 4:17 PM
 */
public interface SchemaService {

    Boolean ensureSystemIndexes(Company company);

    DocumentType resolveDocType(Fortress fortress, String documentType);

    DocumentType resolveDocCode(Fortress fortress, String documentType, Boolean createIfMissing);

    void registerConcepts(Company company, Iterable<TrackResultBean> resultBeans);

    Set<DocumentResultBean> findConcepts(Company company, Collection<String> documents, boolean withRelationships);

    void createDocTypes(Iterable<EntityInputBean> headers, Fortress fortress);

    Collection<DocumentResultBean> getDocumentsInUse(Company company);

    void purge(Fortress fortress);

    boolean ensureUniqueIndexes(Company company, List<TagInputBean> tagInputs, Collection<String> existingIndexes);
}
