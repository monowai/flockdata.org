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

package com.auditbucket.registration.repo.neo4j.model;

import com.auditbucket.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.repo.neo4j.dao.TagDao;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 8:35 PM
 */
@NodeEntity
@TypeAlias("ab.Tag")
public class TagNode implements Tag {
    @GraphId
    Long Id;

//    @RelatedTo(elementClass = CompanyNode.class, type = TagDao.COMPANY_TAGS, direction = Direction.INCOMING)
//    private
//    Company company;

    @Indexed(indexName = "tagCode")
    private String code;

    private String name;

    protected TagNode() {
    }

    public TagNode(TagInputBean tagInput) {
        this();
        setName(tagInput.getName());
        //this.company = company;
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    @JsonIgnore
    // @deprecated
    public Company getCompany() {
        return null;
    }

    @Override
    public void setName(String tagName) {
        this.name = tagName;
        this.code = tagName.toLowerCase().replaceAll("\\s", "");
    }

    @JsonIgnore
    public Long getId() {
        return Id;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String toString() {
        return "TagNode{" +
                "Id=" + Id +
                ", name='" + name + '\'' +
                '}';
    }

    public String getCode() {
        return code;
    }
}
