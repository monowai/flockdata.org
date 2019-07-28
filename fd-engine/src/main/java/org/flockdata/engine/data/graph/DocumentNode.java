/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

package org.flockdata.engine.data.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.flockdata.data.ContentModel;
import org.flockdata.data.Document;
import org.flockdata.data.EntityTag;
import org.flockdata.data.Fortress;
import org.flockdata.data.Segment;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

@NodeEntity
@TypeAlias("DocType")
public class DocumentNode implements Comparable<DocumentNode>, Document {
  @GraphId
  Long id;
  //@Relationship(type = "HAS_CONCEPT", direction = Relationship.OUTGOING)
  @RelatedTo(elementClass = ConceptNode.class, type = "HAS_CONCEPT", direction = Direction.OUTGOING)
  Set<ConceptNode> concepts;
  @RelatedTo(elementClass = FortressSegmentNode.class, type = "USES_SEGMENT", direction = Direction.OUTGOING)
  Set<Segment> segments;
  private String name;
  @Indexed
  private String code;
  @Indexed(unique = true)
  private String companyKey;
  private Boolean searchEnabled;
  private Boolean storeEnabled;
  private Boolean trackEnabled;
  //@Relationship( type = "FORTRESS_DOC", direction = Relationship.OUTGOING)
  @RelatedTo(type = "FORTRESS_DOC", direction = Direction.OUTGOING)
  private FortressNode fortress;
  private String geoQuery;

  private VERSION vstrat = VERSION.FORTRESS;
  // DAT-498
  private EntityTag.TAG_STRUCTURE tagStructure;

  public DocumentNode(Segment segment, ContentModel contentModel) {
    this(segment, contentModel.getDocumentType());
    if (searchEnabled == null && contentModel.isSearchSuppressed() != null) {
      searchEnabled = !contentModel.isSearchSuppressed();
    }
    if (trackEnabled == null && contentModel.isTrackSuppressed() != null) {
      trackEnabled = !contentModel.isTrackSuppressed();
    }
  }

  public DocumentNode(Fortress fortress, ContentModel contentModel) {
    this(fortress.getDefaultSegment(), contentModel);
  }

  protected DocumentNode() {
  }

  public DocumentNode(Fortress fortress, Document docType) {
    this(fortress.getDefaultSegment(), docType);
  }

  public DocumentNode(Segment segment, Document docType) {
    this(segment.getFortress(), docType.getName());
    this.name = docType.getName();
    this.segments = new HashSet<>();
    this.segments.add(segment);
    // ToDo: Parse for injection vulnerabilities.
    // Only admin users can create these and even then only under direction
    this.geoQuery = docType.getGeoQuery(); // DAT-507
    this.searchEnabled = docType.isSearchEnabled();
    this.storeEnabled = docType.isStoreEnabled();
    this.trackEnabled = docType.isTrackEnabled();

    if (docType.getTagStructure() != null) {
      this.tagStructure = docType.getTagStructure();
    }

    if (segment.getFortress() != null) {
      this.companyKey = segment.getCompany().getId() + "." + code;
      setFortress((FortressNode) segment.getFortress());
    }
    if (docType.getVersionStrategy() != null) {
      setVersionStrategy(docType.getVersionStrategy());
    }

  }

  public DocumentNode(String documentName) {
    this(null, documentName);
  }

  /**
   * Only used for testing purposes!
   *
   * @param fortress     could be null - testing only
   * @param documentName usually entity.getType()
   */
  public DocumentNode(Fortress fortress, String documentName) {
    this();
    this.name = documentName;
    this.code = parseCode(fortress, documentName);

    if (fortress != null) {
      this.companyKey = fortress.getCompany().getId() + "." + code;
      setFortress((FortressNode) fortress);
    }

  }

  public DocumentNode(FortressNode fortress, String name, EntityTag.TAG_STRUCTURE tagStructure) {
    this(fortress, name);
    this.tagStructure = tagStructure;
  }

  public static String parseCode(Fortress fortress, String documentType) {
    // Only in testing would the segment be null
    Long fid;
    if (fortress == null || fortress.getId() == null) {
      fid = -1L;
    } else {
      fid = fortress.getId();
    }
    return fid + "." + documentType.toLowerCase().replaceAll("\\s", ".");
  }

  public static String toKey(Fortress fortress, String docType) {
    assert fortress.getCompany() != null;
    return String.valueOf(fortress.getCompany().getId()) + "." + DocumentNode.parseCode(fortress, docType);
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
   *
   * @return internal id that links this Document to a Company
   */
  String getCompanyKey() {
    return companyKey;
  }

  public Collection<ConceptNode> getConcepts() {
    return concepts;
  }

  public Fortress getFortress() {
    return fortress;
  }

  public void setFortress(FortressNode fortress) {
    this.fortress = fortress;
  }

  public void add(ConceptNode concept) {
    if (concepts == null) {
      concepts = new HashSet<>();
    }
    concepts.add(concept);
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

  public int compareTo(DocumentNode o) {
    return o.getCompanyKey().compareTo(companyKey);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DocumentNode)) {
      return false;
    }

    DocumentNode that = (DocumentNode) o;

    if (code != null ? !code.equals(that.code) : that.code != null) {
      return false;
    }
    if (companyKey != null ? !companyKey.equals(that.companyKey) : that.companyKey != null) {
      return false;
    }
    if (id != null ? !id.equals(that.id) : that.id != null) {
      return false;
    }
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

  public EntityTag.TAG_STRUCTURE getTagStructure() {
    return tagStructure;
  }

  // DAT-498
  public void setTagStructure(EntityTag.TAG_STRUCTURE tagFinderClass) {
    this.tagStructure = tagFinderClass;
  }

  public VERSION getVersionStrategy() {
    if (vstrat == null) {
      vstrat = VERSION.FORTRESS;
    }
    return vstrat;
  }

  public DocumentNode setVersionStrategy(VERSION strategy) {
    this.vstrat = strategy;
    return this;
  }

  public Set<Segment> getSegments() {
    return segments;
  }

  public Boolean isSearchEnabled() {
    return searchEnabled;
  }

  public Boolean isStoreEnabled() {
    return storeEnabled;
  }

  public Boolean isTrackEnabled() {
    return trackEnabled;
  }

}
