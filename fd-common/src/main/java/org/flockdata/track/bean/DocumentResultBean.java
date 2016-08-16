/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

package org.flockdata.track.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.model.DocumentType;
import org.flockdata.model.FortressSegment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * User: mike
 * Date: 29/08/14
 * Time: 12:28 PM
 */
public class DocumentResultBean {

    private Long id;
    private String name;
    private Boolean searchSuppressed;
    private Boolean trackSuppressed;
    ArrayList<ConceptResultBean> concepts = new ArrayList<>();
    ArrayList<String> segments = null;

    DocumentResultBean() {
    }

    public DocumentResultBean(DocumentType documentType) {
        this();
        if ( documentType !=null ) {
            this.name = documentType.getName();
            this.id = documentType.getId();
            if ( documentType.getSearchEnabled() !=null)
                this.searchSuppressed = !documentType.getSearchEnabled();
            if ( documentType.getTrackEnabled() !=null) // Suppressed if it's not enabled
                this.trackSuppressed = !documentType.getTrackEnabled();

        }
    }

    public DocumentResultBean(DocumentType documentType, Collection<FortressSegment> segments) {
        this(documentType);
        if (segments!=null) {
            this.segments = new ArrayList<>(segments.size());
            this.segments.addAll(segments.stream().map(FortressSegment::getCode).collect(Collectors.toList()));
        }
    }

    public String getName() {
        return name;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public ArrayList<ConceptResultBean> getConcepts() {
        return concepts;
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
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
        if (this.segments == null)
            segments = new ArrayList<>();
        this.segments.add(segment.getCode());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Boolean getSearchSuppressed() {
        // Null defaults to the fortress
        return searchSuppressed;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Boolean getTrackSuppressed() {
        // Null defaults to the fortress
        return trackSuppressed;
    }
}
