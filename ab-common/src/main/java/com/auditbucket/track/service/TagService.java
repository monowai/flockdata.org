package com.auditbucket.track.service;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;
import org.springframework.scheduling.annotation.Async;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * User: mike
 * Date: 5/09/14
 * Time: 4:30 PM
 */
public interface TagService {

    @Deprecated // Pass the company
    Tag processTag(TagInputBean inputBean);

    Tag processTag(Company company, TagInputBean tagInput);

    @Deprecated // Pass the company
    Collection<TagInputBean> processTags(List<TagInputBean> tagInputs) throws ExecutionException, InterruptedException;

    Collection<TagInputBean> processTags(Company company, List<TagInputBean> tagInputs) throws ExecutionException, InterruptedException;

    @Async
    Future<Collection<TagInputBean>> makeTags(Company company, List<TagInputBean> tagInputs) throws ExecutionException, InterruptedException;

    Tag findTag(Company company, String tagName);

    @Deprecated // Pass the company
    Tag findTag(String tagName);

    @Deprecated
    Collection<Tag> findDirectedTags(Tag startTag);

    @Deprecated // Pass the company
    Collection<Tag> findTags(String index);

    Collection<Tag> findTags(Company company, String index);

    Tag findTag(Company company, String tagName, String index);

    Collection<String> getExistingIndexes();

    void createTagsNoRelationships(Company company, List<TagInputBean> tagInputs) throws DatagioException, IOException, ExecutionException, InterruptedException;

    void purgeUnusedConcepts(Company company);

    void purgeType(Company company, String type);
}
