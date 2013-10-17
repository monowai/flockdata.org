/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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

package com.auditbucket.engine.repo.neo4j;

import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.AuditTag;
import com.auditbucket.engine.repo.neo4j.model.AuditHeaderNode;
import com.auditbucket.engine.repo.neo4j.model.AuditTagRelationship;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.repo.neo4j.model.TagNode;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 28/06/13
 * Time: 10:56 PM
 */
public interface AuditTagRepo extends GraphRepository<AuditTagRelationship> {

//    @Query(elementClass = TagNode.class,
//            value = "start tag=node({0}) " +
//                    "match tag-[tags:auditTag]->auditHeader " +
//                    "where tags.tagType={1} return tags")
//    Set<Tag> findTagValues(Long tagId, String tagType);


}
