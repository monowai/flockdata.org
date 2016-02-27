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

    //@Relationship( type = "FORTRESS_DOC", direction = Relationship.OUTGOING)
    @RelatedTo( type = "FORTRESS_DOC", direction = Direction.OUTGOING)
    private Fortress fortress;

    //@Relationship(type = "HAS_CONCEPT", direction = Relationship.OUTGOING)
    @RelatedTo(elementClass = Concept.class,  type = "HAS_CONCEPT", direction = Direction.OUTGOING)
    Set<Concept> concepts;

    @RelatedTo(elementClass = DocumentType.class,  type = "PARENT", direction = Direction.INCOMING)
    DocumentType parent;

    private String geoQuery;

    private VERSION vstrat = VERSION.FORTRESS;

    /**
     *
     * Set the version strategy on a per DocumentType basis
     *
     * Enable version control when fortress.storeEnabled== false
     * Suppress when your fortress.storeEnabled== true and you don't want to version
     * Fortress (default) means use whatever the fortress default is
     *
     */
    public enum VERSION {
        FORTRESS, ENABLE, DISABLE
    }

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
        if ( docType.getTagStructure()!= null)
            this.tagStructure = docType.getTagStructure();

        if ( fortress !=null ){
            this.companyKey = fortress.getCompany().getId() + "." + code;
            setFortress(fortress);
        }
        if ( docType.getVersionStrategy()!=null )
            setVersionStrategy(docType.getVersionStrategy());
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
        this.code = parseCode(fortress, documentType.getName());
        this.tagStructure = documentType.getTagStructure();
        if ( fortress !=null ){
            this.companyKey = fortress.getCompany().getId() + "." + code;
            setFortress(fortress);
        }

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
                ", fortress=" + fortress +
                '}';
    }

    public static String parseCode(Fortress fortress, String documentType) {
        // Only in testing would the fortress be null
        Long fid ;
        if ( fortress == null || fortress.getId() == null )
            fid = -1l;
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
}
