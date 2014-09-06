/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.dao;

import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;

import java.util.Collection;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 8:12 PM
 */
public interface TagDao {

    public Collection<TagInputBean> save(Company company, Iterable<TagInputBean> tags);

    public Collection<TagInputBean> save(Company company, Iterable<TagInputBean> tagInputs, boolean suppressRelationships);

    public Tag save(Company company, TagInputBean tagInput);

    /**
     * Locates a tag
     *
     *
     *
     * @param company
     * @param tagName name to find
     * @param index
     * @return the tag if it exists or null
     */
    public Tag findOne(Company company, String tagName, String index);

    public Collection<Tag> findDirectedTags(Tag startTag, Company company, boolean b);

    public Collection<Tag> findTags(Company company);

    public Collection<Tag> findTags(Company company, String index);

    public Collection<String> getExistingIndexes();

    public void purgeUnusedConcepts(Company company);

    public void purge(Company company, String type);

}
