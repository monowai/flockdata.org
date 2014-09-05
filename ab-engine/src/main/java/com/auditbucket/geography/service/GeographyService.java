package com.auditbucket.geography.service;

import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * User: mike
 * Date: 27/04/14
 * Time: 11:47 AM
 */
@Service
@Transactional
public class GeographyService {
    @Autowired
    com.auditbucket.track.service.TagService tagService;
    public Collection<Tag> findCountries(Company company) {
        return tagService.findTags(company, "Country");

    }
}
