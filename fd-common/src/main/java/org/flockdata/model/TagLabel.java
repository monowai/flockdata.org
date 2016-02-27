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

package org.flockdata.model;

import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

/**
 *
 * Represents part of the tracked schema. This is a label that exists within the graph
 *
 * User: Mike Holdsworth
 * Date: 30/06/13
 * Time: 10:02 AM
 */
@NodeEntity
@TypeAlias("TagLabel")
public class TagLabel {

    @GraphId
    Long id;

    private String name;

    @Indexed(unique = true)
    private String companyKey;

    //@Relationship( type = "TAG_INDEX", direction = Relationship.OUTGOING)
    @RelatedTo( type = "TAG_INDEX", direction = Direction.OUTGOING)
    private Company company;

    protected TagLabel() {
    }

    public TagLabel(Company company, String name) {
        this();
        this.name = name;
        this.companyKey = parseTagLabel(company, name);

    }

    public static String parseTagLabel(Company company, String label) {
        return company.getId() + ".t." + label.toLowerCase().replaceAll("\\s", "");
    }


    public void setCompany(Company company) {
        this.company = company;
    }

    public String getName() {
        return name;
    }

    public Company getCompany() {
        return company;
    }

    @Override
    public String toString() {
        return "DocumentType{" +
                "id=" + id +
                ", company=" + company +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TagLabel)) return false;

        TagLabel that = (TagLabel) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (companyKey != null ? !companyKey.equals(that.companyKey) : that.companyKey != null) return false;
        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (companyKey != null ? companyKey.hashCode() : 0);
        return result;
    }
}
