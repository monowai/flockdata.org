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

import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;

import java.io.Serializable;

@NodeEntity
@TypeAlias(value ="FDCompany")
public class Company implements Serializable {
    @GraphId
    Long id;

    @Indexed
    private String name;

    @Indexed(unique = true)
    private String code;

    @Indexed
    String apiKey;

    protected Company() {
    }

    public Company(String companyName) {
        this(companyName, null);
    }

    public Company(String companyName, String apiKey) {
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
    public String getCode() {
        return code;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Company)) return false;

        Company that = (Company) o;

        if (apiKey != null ? !apiKey.equals(that.apiKey) : that.apiKey != null) return false;
        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (apiKey != null ? apiKey.hashCode() : 0);
        return result;
    }


}
