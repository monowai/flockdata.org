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

package org.flockdata.track.bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.track.model.Concept;
import org.flockdata.track.model.DocumentType;

import java.util.ArrayList;

/**
 * User: mike
 * Date: 29/08/14
 * Time: 12:28 PM
 */
public class DocumentResultBean {

    private Long id;

    public String getName() {
        return name;
    }

    public String getFortressName() {
        return fortressName;
    }

    public String getFortressCode() {
        return fortressCode;
    }

    private String name;
    private String fortressName;
    private String fortressCode;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public ArrayList<Concept> getConcepts() {
        return concepts;
    }

    ArrayList<Concept> concepts = new ArrayList<>();

    DocumentResultBean() {
    }

    public DocumentResultBean(DocumentType documentType) {
        this();
        this.name = documentType.getName();
//        code = documentType.getCode();
        fortressName = documentType.getFortress().getName();
        fortressCode = documentType.getFortress().getCode();
        this.id = documentType.getId();

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void add(Concept concept) {

        if (concepts == null)
            concepts = new ArrayList<>();
        concepts.add(concept);
    }
//    @Override
//    public int compareTo(DocumentResultBean o) {
//        return o.getName().compareTo(name) + o.getFortressCode().compareTo(fortressCode);
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentResultBean)) return false;

        DocumentResultBean that = (DocumentResultBean) o;

        if (fortressCode != null ? !fortressCode.equals(that.fortressCode) : that.fortressCode != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (fortressCode != null ? fortressCode.hashCode() : 0);
        return result;
    }
}
