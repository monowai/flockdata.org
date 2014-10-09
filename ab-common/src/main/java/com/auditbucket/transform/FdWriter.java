package com.auditbucket.transform;

import com.auditbucket.helper.FlockException;
import com.auditbucket.registration.bean.SystemUserResultBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.track.bean.CrossReferenceInputBean;
import com.auditbucket.track.bean.EntityInputBean;

import java.util.Collection;
import java.util.List;

/**
 * User: mike
 * Date: 7/10/14
 * Time: 12:18 PM
 */
public interface FdWriter {
    /**
     * Resolve the currently logged in user
     * @return su
     */
    SystemUserResultBean me();

    String flushTags(List<TagInputBean> tagInputBeans) throws FlockException;

    String flushEntities(List<EntityInputBean> entityBatch) throws FlockException;

    int flushXReferences(List<CrossReferenceInputBean> referenceInputBeans) throws FlockException;



    /**
     * if True, then the writer will not persist changes
     * @return
     */
    boolean isSimulateOnly();

    Collection<com.auditbucket.registration.model.Tag> getCountries()  throws FlockException ;
}