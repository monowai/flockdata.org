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

package org.flockdata.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.service.EntityService;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.ArrayList;
import java.util.Collection;

/**
 * User: Mike Holdsworth
 * Date: 30/06/13
 * Time: 10:06 AM
 */
@NodeEntity
@TypeAlias("DocType")
public class DocumentType  implements Comparable<DocumentType> {
    @GraphId
    Long id;

    private String name;

    @Indexed
    private String code;

    @Indexed(unique = true)
    private String companyKey;

    //@Relationship( type = "FORTRESS_DOC", direction = Relationship.OUTGOING)
    @RelatedTo( type = "FORTRESS_DOC", direction = Direction.OUTGOING)
    //@Fetch
    private Fortress fortress;

    //@Relationship(type = "HAS_CONCEPT", direction = Relationship.OUTGOING)
    @RelatedTo(elementClass = Concept.class,  type = "HAS_CONCEPT", direction = Direction.OUTGOING)
    Collection<Concept> concepts;

    @RelatedTo(elementClass = DocumentType.class,  type = "PARENT", direction = Direction.INCOMING)
    DocumentType parent;

    private String geoQuery;

    // DAT-498
    private EntityService.TAG_STRUCTURE tagStructure;

    protected DocumentType() {
    }

    public DocumentType(Fortress fortress, DocumentTypeInputBean docType) {
        this(fortress, docType.getName());
        this.name = docType.getName();
        // ToDo: Parse for injection vulnerabilities.
        // Only admin users can create these and even then only under direction
        this.geoQuery = docType.getGeoQuery(); // DAT-507

        if ( fortress !=null ){
            this.companyKey = fortress.getCompany().getId() + "." + code;
            setFortress(fortress);
        }


    }

    public DocumentType(String documentName) {
        this(null, documentName);
    }

    /**
     *
     * @param fortress      System that owns the documentType
     * @param documentType  The input that will create a real DocumentType
     */
    public DocumentType(Fortress fortress, DocumentType documentType) {
        this.name = documentType.getName();
        this.code = parse(fortress, documentType.getName());
        this.tagStructure = documentType.getTagStructure();
        if ( fortress !=null ){
            this.companyKey = fortress.getCompany().getId() + "." + code;
            setFortress(fortress);
        }

    }

    public DocumentType(Fortress fortress, String documentName) {
        this();
        this.name = documentName;
        this.code = parse(fortress, documentName);

        if ( fortress !=null ){
            this.companyKey = fortress.getCompany().getId() + "." + code;
            setFortress(fortress);
        }

    }

    public DocumentType(DocumentType document) {
        this(document.getFortress(), document.getName());
        this.id = document.getId();
    }

    public DocumentType(Fortress fortress, String name, EntityService.TAG_STRUCTURE tagStructure) {
        this(fortress, name);
        this.tagStructure = tagStructure ;
    }

    public void setFortress(Fortress fortress) {
        this.fortress = fortress;
    }

    public String getName() {
        return name;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }


    /**
     * used to create a unique key index for a company+docType combo
     */
    public String getCompanyKey() {
        return companyKey;
    }

    public Collection<org.flockdata.model.Concept> getConcepts() {
        return concepts;
    }

    public Fortress getFortress() {
        return fortress;
    }

    public void add(Concept concept) {
        if ( concepts == null )
            concepts = new ArrayList<>();
        concepts.add( concept);
    }

    @Override
    public String toString() {
        return "DocumentType{" +
                "id=" + id +
                ", fortress=" + fortress +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                '}';
    }

    public static String parse(Fortress fortress, String documentType) {
        return fortress.getId() + "."+ documentType.toLowerCase().replaceAll("\\s", ".");
    }

    public int compareTo(DocumentType o) {
        return o.getCompanyKey().compareTo(companyKey);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentType)) return false;

        DocumentType that = (DocumentType) o;

        if (code != null ? !code.equals(that.code) : that.code != null) return false;
        if (companyKey != null ? !companyKey.equals(that.companyKey) : that.companyKey != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        return !(name != null ? !name.equals(that.name) : that.name != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (companyKey != null ? companyKey.hashCode() : 0);
        return result;
    }


    public String getGeoQuery() {
        return geoQuery;
    }

    public void setGeoQuery(String geoQuery) {
        this.geoQuery = geoQuery;
    }

    public EntityService.TAG_STRUCTURE getTagStructure() {
        return tagStructure;
    }

    // DAT-498
    public void setTagStructure(EntityService.TAG_STRUCTURE tagFinderClass) {
        this.tagStructure = tagFinderClass;
    }

    public DocumentType getParent() {
        return parent;
    }

    public void setParent(DocumentType parent) {
        this.parent = parent;
    }

    @JsonIgnore
    public boolean hasParent() {
        return parent!=null;
    }

}
