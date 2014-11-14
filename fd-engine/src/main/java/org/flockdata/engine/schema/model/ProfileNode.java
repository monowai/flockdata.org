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

package org.flockdata.engine.schema.model;

import org.flockdata.company.model.FortressNode;
import org.flockdata.profile.model.ProfileConfiguration;
import org.flockdata.registration.model.Fortress;
import org.flockdata.track.model.DocumentType;
import org.flockdata.track.model.Profile;
import org.flockdata.transform.ColumnDefinition;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.Collection;
import java.util.Map;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 4:30 PM
 */
@NodeEntity(useShortNames = true)
@TypeAlias("_Profile")
public class ProfileNode implements Profile {

    @GraphId
    private Long id;

    @Indexed(unique = true)
    private String profileKey;

    @RelatedTo(elementClass = FortressNode.class, type = "FORTRESS_PROFILE", direction = Direction.OUTGOING)
    private Fortress fortress;

    @RelatedTo(elementClass = DocumentTypeNode.class, type = "DOCUMENT_PROFILE", direction = Direction.OUTGOING)
    private DocumentType document;

    private String content;
    private ProfileConfiguration.ContentType contentType;
    private String clazz;
    private char delimiter;
    private Collection<String> strategyCols;
    private boolean hasHeader;
    private String fortressUser;
    private boolean entityOnly;
    private String entityKey;
    private boolean archiveTags;
    private String event;
    private Map<String, ColumnDefinition> columns;

    public ProfileNode() {}

    public ProfileNode(Fortress fortress, DocumentType documentType) {
        this.fortress = fortress;
        this.document = documentType;
        this.profileKey = parseKey(fortress, documentType);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getProfileKey() {
        return profileKey;
    }

    public static String parseKey(Fortress fortress, DocumentType documentType) {
        return fortress.getId() +"-"+documentType.getId();
    }

    public Long getId() {
        return id;
    }

//    @Override
//    public String getDocumentName() {
//        return document.getCode();
//    }
//
//    @Override
//    public ContentType getContentType() {
//        return contentType;
//    }
//
//    @Override
//    public void setColumns(Map<String, ColumnDefinition> columns) {
//        this.columns = columns;
//    }
//
//    @Override
//    public Map<String, ColumnDefinition> getContent() {
//        return columns;
//    }
//
//    @Override
//    public String getClazz() {
//        return clazz;
//    }
//
//    @Override
//    public char getDelimiter() {
//        return delimiter;
//    }
//
//    @Override
//    public String getTagOrEntity() {
//        return "";
//    }
//
//    @Override
//    public String getFortressName() {
//        return fortress.getCode();
//    }
//
//    @Override
//    public boolean hasHeader() {
//        return hasHeader;
//    }
//
//    @Override
//    public Mappable getMappable() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
//        return null;
//    }
//
//    @Override
//    public String getFortressUser() {
//        return fortressUser;
//    }
//
//    @Override
//    public boolean isEntityOnly() {
//        return entityOnly;
//    }
//
//    @Override
//    public String getEntityKey() {
//        return entityKey;
//    }
//
//    @Override
//    public Collection<String> getStrategyCols() {
//        return strategyCols;
//    }
//
//    @Override
//    public boolean isArchiveTags() {
//        return archiveTags;
//    }
//
//    @Override
//    public String getEvent() {
//        return event;
//    }

//    public void setFromProfile(ProfileConfiguration fromProfile) {
//        this.event = fromProfile.getEvent();
//        this.archiveTags = fromProfile.isArchiveTags();
//        this.clazz = fromProfile.getClazz();
//        this.contentType = fromProfile.getContentType();
//        this.delimiter = fromProfile.getDelimiter();
//        this.strategyCols = fromProfile.getStrategyCols();
//        this.entityKey = fromProfile.getEntityKey();
//        this.fortressUser = fromProfile.getFortressUser();
//        this.columns = fromProfile.getContent();
//        this.entityOnly = fromProfile.isEntityOnly();
//
//    }
}
