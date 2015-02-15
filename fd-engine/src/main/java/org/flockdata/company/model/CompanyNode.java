/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.company.model;

import org.flockdata.registration.model.Company;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.Labels;
import org.springframework.data.neo4j.annotation.NodeEntity;

import java.util.ArrayList;

@NodeEntity
@TypeAlias(value ="FDCompany")
public class CompanyNode implements Company {
    @GraphId
    Long id;

    @Indexed
    private String name;

    @Indexed(unique = true)
    private String code;

    @Indexed
    String apiKey;

    @Labels
    private ArrayList<String> labels = new ArrayList<>();


    protected CompanyNode() {
    }

    public CompanyNode(String companyName) {
        this(companyName, null);
    }

    public CompanyNode(String companyName, String apiKey) {
        this();
        setName(companyName);
        this.apiKey = apiKey;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        if (code == null)
            this.code = name.toLowerCase().replaceAll("\\s", "");
    }

    @Override
    public String getApiKey() {
        return this.apiKey;
    }


    @Override
    public String toString() {
        return "CompanyNode{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                '}';
    }

    // Lower case, no spaces
    @JsonIgnore
    public String getCode() {
        return code;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompanyNode)) return false;

        CompanyNode that = (CompanyNode) o;

        if (apiKey != null ? !apiKey.equals(that.apiKey) : that.apiKey != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (apiKey != null ? apiKey.hashCode() : 0);
        return result;
    }

    public ArrayList<String> getLabels() {
        return labels;
    }

    public void setLabels(ArrayList<String> labels) {
        this.labels = labels;
    }

}
