package com.auditbucket.engine.service;

import com.auditbucket.engine.repo.neo4j.dao.TagDaoNeo4j;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * User: mike
 * Date: 26/09/14
 * Time: 6:43 PM
 */

@EnableRetry
@Service
@Transactional
public class TagRetryService {

    @Autowired
    private TagDaoNeo4j tagDao;

    @Retryable(include = Exception.class, maxAttempts = 12, backoff = @Backoff(delay = 50, maxDelay = 400))
    public void track(Company company, List<TagInputBean> tagInputBeans) {
        tagDao.save(company, tagInputBeans, true);


    }

}
