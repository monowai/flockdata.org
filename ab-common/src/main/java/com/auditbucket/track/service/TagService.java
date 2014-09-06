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

    Tag createTag(Company company, TagInputBean tagInput);

    public void createTags(Company company, List<TagInputBean> tagInputs) throws DatagioException, IOException, ExecutionException, InterruptedException;

    @Async
    public Future<Collection<Tag>> makeTags(Company company, List<TagInputBean> tagInputs) throws ExecutionException, InterruptedException;

    public Tag findTag(Company company, String tagName);

    @Deprecated // Pass the company
    public Tag findTag(String tagName);

    @Deprecated
    public Collection<Tag> findDirectedTags(Tag startTag);

    public Collection<Tag> findTags(Company company, String index);

    public Tag findTag(Company company, String tagName, String index);

    public Collection<String> getExistingIndexes();

    public void purgeUnusedConcepts(Company company);

    public void purgeType(Company company, String type);
}
