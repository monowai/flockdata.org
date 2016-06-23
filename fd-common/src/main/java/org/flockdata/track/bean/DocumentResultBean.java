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

package org.flockdata.track.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.model.DocumentType;
import org.flockdata.model.FortressSegment;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * User: mike
 * Date: 29/08/14
 * Time: 12:28 PM
 */
public class DocumentResultBean {

    private Long id;
    private String name;
    ArrayList<ConceptResultBean> concepts = new ArrayList<>();
    ArrayList<String>segments = new ArrayList<>();

    DocumentResultBean() {
    }

    public String getName() {
        return name;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public ArrayList<ConceptResultBean> getConcepts() {
        return concepts;
    }

    public DocumentResultBean(DocumentType documentType) {
        this();
        this.name = documentType.getName();
        this.id = documentType.getId();
        this.segments.addAll(documentType.getSegments().stream().map(FortressSegment::getCode).collect(Collectors.toList()));
    }

    @JsonIgnore
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void add(ConceptResultBean concept) {
        if (concepts == null)
            concepts = new ArrayList<>();
        concepts.add(concept);
    }

    public ArrayList<String> getSegments() {
        return segments;
    }

    @Override
    public String toString() {
        return "DocumentResultBean{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentResultBean)) return false;

        DocumentResultBean that = (DocumentResultBean) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        return !(name != null ? !name.equals(that.name) : that.name != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    public void addSegment(FortressSegment segment) {
        this.segments.add(segment.getCode());
    }
}
