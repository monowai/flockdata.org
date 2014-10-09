package com.auditbucket.geography.service;

import com.auditbucket.registration.model.Tag;
import com.auditbucket.track.service.TagService;
import com.auditbucket.registration.model.Company;

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
    TagService tagService;
    public Collection<Tag> findCountries(Company company) {
        return tagService.findTags(company, "Country");

    }
}
