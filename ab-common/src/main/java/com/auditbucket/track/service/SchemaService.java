package com.auditbucket.track.service;

import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.track.bean.DocumentResultBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.DocumentType;
import org.springframework.scheduling.annotation.Async;

import java.util.Collection;
import java.util.Set;

/**
 * User: mike
 * Date: 5/09/14
 * Time: 4:17 PM
 */
public interface SchemaService {
    @Async
    void ensureSystemIndexes(Company company);

    DocumentType resolveDocType(Fortress fortress, String documentType);

    DocumentType resolveDocType(Fortress fortress, String documentType, Boolean createIfMissing);

    void registerConcepts(Company company, Iterable<TrackResultBean> resultBeans);

    Set<DocumentResultBean> findConcepts(Company company, Collection<String> documents, boolean withRelationships);

    void createDocTypes(Iterable<MetaInputBean> headers, Fortress fortress);

    Collection<DocumentResultBean> getCompanyDocumentsInUse(Company company);

    void purge(Fortress fortress);
}
