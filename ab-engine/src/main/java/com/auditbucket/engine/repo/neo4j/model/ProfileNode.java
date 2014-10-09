package com.auditbucket.engine.repo.neo4j.model;

import com.auditbucket.profile.model.ProfileConfiguration;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.track.model.DocumentType;
import com.auditbucket.track.model.Profile;
import com.auditbucket.transform.ColumnDefinition;
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
//    public String getDocumentType() {
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
//    public Map<String, ColumnDefinition> getColumns() {
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
//    public String getFortress() {
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
//        this.columns = fromProfile.getColumns();
//        this.entityOnly = fromProfile.isEntityOnly();
//
//    }
}
