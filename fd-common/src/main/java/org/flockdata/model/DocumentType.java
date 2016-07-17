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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * FD has a document oriented view of information.
 *
 * Classifies a type of Entity as being of a "DocumentType"
 *
 * For example, Invoice, Customer, Person etc.
 *
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

    private Boolean searchEnabled;

    private Boolean storeEnabled;

    //@Relationship( type = "FORTRESS_DOC", direction = Relationship.OUTGOING)
    @RelatedTo( type = "FORTRESS_DOC", direction = Direction.OUTGOING)
    private Fortress fortress;

    //@Relationship(type = "HAS_CONCEPT", direction = Relationship.OUTGOING)
    @RelatedTo(elementClass = Concept.class,  type = "HAS_CONCEPT", direction = Direction.OUTGOING)
    Set<Concept> concepts;

    @RelatedTo(elementClass = FortressSegment.class,  type = "USES_SEGMENT", direction = Direction.OUTGOING)
    Set<FortressSegment> segments ;


    @RelatedTo(elementClass = DocumentType.class,  type = "PARENT", direction = Direction.INCOMING)
    DocumentType parent;

    private String geoQuery;

    private VERSION vstrat = VERSION.FORTRESS;

    /**
     *
     * Set the version strategy on a per DocumentType basis
     *
     * Enable version control when segment.storeEnabled== false
     * Suppress when your segment.storeEnabled== true and you don't want to version
     * Fortress (default) means use whatever the segment default is
     *
     */
    public enum VERSION {
        FORTRESS, ENABLE, DISABLE
    }

        // DAT-498
    private EntityService.TAG_STRUCTURE tagStructure;

    protected DocumentType() {
    }

    public DocumentType(FortressSegment segment, DocumentTypeInputBean docType) {
        this(segment.getFortress(), docType.getName());
        this.name = docType.getName();
        this.segments = new HashSet<>();
        this.segments.add(segment);
        // ToDo: Parse for injection vulnerabilities.
        // Only admin users can create these and even then only under direction
        this.geoQuery = docType.getGeoQuery(); // DAT-507
        this.searchEnabled = docType.isSearchEnabled();
        this.storeEnabled = docType.isStoreEnabled();

        if ( docType.getTagStructure()!= null)
            this.tagStructure = docType.getTagStructure();

        if ( segment.getFortress() !=null ){
            this.companyKey = segment.getCompany().getId() + "." + code;
            setFortress(segment.getFortress());
        }
        if ( docType.getVersionStrategy()!=null )
            setVersionStrategy(docType.getVersionStrategy());

    }

    public DocumentType(String documentName) {
        this(null, documentName);
    }

    /**
     * Only used for testing purposes!
     * @param fortress      could be null - testing only
     * @param documentName  usually entity.getType()
     */
    public DocumentType(Fortress fortress, String documentName) {
        this();
        this.name = documentName;
        this.code = parseCode(fortress, documentName);

        if ( fortress !=null ){
            this.companyKey = fortress.getCompany().getId() + "." + code;
            setFortress(fortress);
        }

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

    public Collection<Concept> getConcepts() {
        return concepts;
    }

    public Fortress getFortress() {
        return fortress;
    }

    public void add(Concept concept) {
        if ( concepts == null )
            concepts = new HashSet<>();
        concepts.add( concept);
    }

    @Override
    public String toString() {
        return "DocumentType{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", segment=" + fortress +
                '}';
    }

    public static String parseCode(Fortress fortress, String documentType) {
        // Only in testing would the segment be null
        Long fid ;
        if ( fortress == null || fortress.getId() == null )
            fid = -1L;
        else
            fid = fortress.getId();
        return fid+ "."+ documentType.toLowerCase().replaceAll("\\s", ".");
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

    public static String toKey(Fortress fortress, String docType) {
        assert fortress.getCompany()!=null;
        return String.valueOf(fortress.getCompany().getId()) + "." + DocumentType.parseCode(fortress, docType);
    }

    public VERSION getVersionStrategy() {
        if ( vstrat == null )
            vstrat = VERSION.FORTRESS;
        return vstrat;
    }

    public DocumentType setVersionStrategy(VERSION strategy) {
        this.vstrat = strategy;
        return this;
    }

    public Set<FortressSegment>getSegments (){
        return segments;
    }

    public Boolean getSearchEnabled() {
        return searchEnabled;
    }

    public Boolean getStoreEnabled() {
        return storeEnabled;
    }
}
