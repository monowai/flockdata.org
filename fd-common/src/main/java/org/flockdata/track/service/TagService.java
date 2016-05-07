/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.track.service;

import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.Company;
import org.flockdata.model.Tag;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;

import java.util.Collection;
import java.util.Map;

/**
 * User: mike
 * Date: 5/09/14
 * Time: 4:30 PM
 */
public interface TagService {

    Tag createTag(Company company, TagInputBean tagInput) throws FlockException;

    Collection<TagResultBean> createTags(Company company, Collection<TagInputBean> tagInputs) throws FlockException;

    Tag findTag(Company company, String keyPrefix, String tagCode);

    Collection<Tag> findDirectedTags(Tag startTag);

    Collection<Tag> findTags(Company company, String label);

    Collection<TagResultBean> findTagResults(Company company, String label) ;

    Tag findTag(Company company, String label, String keyPrefix, String tagCode);

    Tag findTag(Company company, String label, String keyPrefix, String tagCode, boolean inflate) throws NotFoundException;

    void createAlias(Company company, Tag tag, String forLabel, String aliasKeyValue);

    void createAlias(Company company, Tag tag, String forLabel, AliasInputBean aliasInput );

    Collection<AliasInputBean> findTagAliases(Company company, String label, String keyPrefix, String sourceTag) throws NotFoundException;

    Map<String, Collection<TagResultBean>> findTags(Company company, String sourceLabel, String sourceCode, String relationship, String targetLabel) throws NotFoundException;

    Collection<Tag> findTag(Company company, String code);
}
