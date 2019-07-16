/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.tag.service;

import java.util.Collection;
import java.util.Map;
import org.flockdata.data.Company;
import org.flockdata.data.Tag;
import org.flockdata.engine.data.graph.TagNode;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.track.bean.FdTagResultBean;

/**
 * @author mholdsworth
 * @since 5/09/2014
 */
public interface TagService {

    FdTagResultBean createTag(Company company, TagInputBean tagInput) throws FlockException;

    Collection<FdTagResultBean> createTags(Company company, Collection<TagInputBean> tagInputs) throws FlockException;

    Tag findTag(Company company, String keyPrefix, String tagCode);

    Collection<Tag> findDirectedTags(Tag startTag);

    Collection<TagResultBean> findTags(Company company);

    Collection<Tag> findTags(Company company, String label);

    Collection<FdTagResultBean> findTagResults(Company company, String label);

    Tag findTag(Company company, String label, String keyPrefix, String tagCode);

    Tag findTag(Company company, String label, String keyPrefix, String tagCode, boolean inflate) throws NotFoundException;

    void createAlias(Company company, Tag tag, String forLabel, String aliasKeyValue);

    void createAlias(Company company, Tag tag, String forLabel, AliasInputBean aliasInput);

    Collection<AliasInputBean> findTagAliases(Company company, String label, String keyPrefix, String sourceTag) throws NotFoundException;

    Map<String, Collection<FdTagResultBean>> findTags(Company company, String sourceLabel, String sourceCode, String relationship, String targetLabel) throws NotFoundException;

    Collection<TagNode> findTag(Company company, String code);
}
