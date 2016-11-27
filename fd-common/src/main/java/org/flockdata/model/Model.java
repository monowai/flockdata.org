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

import org.flockdata.track.bean.TrackResultBean;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

/**
 * ContentModel persistence.
 * @author mholdsworth
 * @since 3/10/2014
 * @tag Node, ContentModel
 */
@NodeEntity()
@TypeAlias("Model")
public class Model {
    @GraphId
    private Long id;

    @Indexed(unique = true)
    private String key;

    @Indexed
    private String code;

    //@Relationship(type = "FORTRESS_PROFILE")
    @RelatedTo(type = "FORTRESS_MODEL")
    private Fortress fortress;

    private String fortressName;

    //@Relationship( type = "DOCUMENT_PROFILE")
    @RelatedTo( type = "DOCUMENT_MODEL")
    private DocumentType document;

    private String documentName;

    @RelatedTo(type = "COMPANY_MODEL")
    private Company company;

    private String name;


    Model() {}

    public Model(TrackResultBean trackResultBean, Fortress fortress, DocumentType documentType) {
        this();
        this.company = trackResultBean.getCompany();
        this.fortress = fortress;
        this.document = documentType;
        this.fortressName = fortress.getName();
        this.documentName = (documentType==null?null:documentType.getName());
        this.name = trackResultBean.getEntity().getName();
        this.key = trackResultBean.getKey();

    }

    public Model(TrackResultBean trackResult, String code) {
        this.company = trackResult.getCompany();
        this.document = trackResult.getDocumentType();
        this.name = trackResult.getEntity().getName();
        this.key = trackResult.getKey();
        this.code = code;
    }

    public Company getCompany() {
        return company;
    }

    public String getKey() {
        return key;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Fortress getFortress() {
        return fortress;
    }

    public DocumentType getDocument() {
        return document;
    }

    public String getCode() {
        return code;
    }

    public String getFortressName() {
        return fortressName;
    }

    public String getDocumentName() {
        return documentName;
    }
}
