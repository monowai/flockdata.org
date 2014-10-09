package com.auditbucket.profile;

import com.auditbucket.profile.model.Mappable;
import com.auditbucket.profile.model.ProfileConfiguration;
import com.auditbucket.transform.ColumnDefinition;
import com.auditbucket.transform.FdReader;
import com.auditbucket.transform.TagMapper;
import com.auditbucket.transform.csv.CsvEntityMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * See also ImportParamsDeserializer for mapping logic
 * User: mike
 * Date: 28/04/14
 * Time: 8:47 AM
 */
@JsonDeserialize(using = ImportProfileDeserializer.class)
public class ImportProfile implements ProfileConfiguration {

    private String documentType;
    private ContentType contentType;
    private String tagOrEntity;
    private String clazz = null;
    private String staticDataClazz;
    private char delimiter = ',';
    private String fortress = null;
    private boolean header = true;
    private String fortressUser;
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(ImportProfile.class);
    private boolean entityOnly;
    private boolean archiveTags = true;

    private Map<String, ColumnDefinition> columns;
    private FdReader staticDataResolver;
    private String entityKey;
    private String event = null;

    public ImportProfile() {

    }

//    public ImportProfile(IStaticDataResolver restClient) {
//        this.restClient = restClient;
//    }
//
//    public ImportProfile(String clazz, IStaticDataResolver restClient) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
//        this(restClient);
//        this.clazz = clazz;
//        this.contentType = ((Mappable) Class.forName(getClazz()).newInstance()).getImporter();
//
//    }

    public void setHeader(boolean header) {
        this.header = header;
    }

    public void setFortressUser(String fortressUser) {
        this.fortressUser = fortressUser;
    }

    public void setColumns(Map<String, ColumnDefinition> columns) {
        this.columns = columns;
    }

    @Override
    public String toString() {
        return "ImportParams{" +
                "documentType='" + documentType + '\'' +
                ", contentType=" + contentType +
                ", tagOrEntity='" + tagOrEntity + '\'' +
                ", clazz='" + clazz + '\'' +
                ", delimiter=" + delimiter +
                '}';
    }

    @Override
    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    @Override
    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    @Override
    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    @Override
    public char getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(char delimiter) {
        this.delimiter = delimiter;
    }

    @Override
    public String getTagOrEntity() {
        return tagOrEntity;
    }

    public void setTagOrEntity(String tagOrEntity) {
        if ( tagOrEntity.equalsIgnoreCase("track")) // Backward compatibility. We should look to remove.
            tagOrEntity = "entity";
        this.tagOrEntity = tagOrEntity;
    }

    @Override
    public String getFortress() {
        return fortress;
    }

    public void setFortress(String fortress) {
        this.fortress = fortress;
    }

    @Override
    public boolean hasHeader() {
        return header;
    }

    @Override
    public Mappable getMappable() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Mappable mappable = null;

        if (!(clazz == null || clazz.equals("")))
            mappable = (Mappable) Class.forName(getClazz()).newInstance();
        else if (getTagOrEntity().equalsIgnoreCase("entity")) {
            mappable = CsvEntityMapper.newInstance(this);
        } else if (getTagOrEntity().equalsIgnoreCase("tag")) {
            mappable = TagMapper.newInstance(this);
        } else
            logger.error("Unable to determine the implementing handler");


        return mappable;

    }

    @Override
    public String getFortressUser() {
        return fortressUser;
    }

    @Override
    public boolean isEntityOnly() {
        return entityOnly;
    }

    public void setEntityOnly(boolean entityOnly) {
        this.entityOnly = entityOnly;
    }

    public ColumnDefinition getColumnDef(String column) {
        if (columns == null)
            return null;
        return columns.get(column);
    }

    public void setStaticDataResolver(FdReader staticDataResolver) {
        this.staticDataResolver = staticDataResolver;
    }

    public void setEntityKey(String entityKey) {
        this.entityKey = entityKey;
    }

    @Override
    public String getEntityKey() {
        return entityKey;
    }

    public Map<String, ColumnDefinition> getColumns() {
        return columns;
    }

    @Override
    public Collection<String> getStrategyCols() {
        Map<String, ColumnDefinition> columns = getColumns();

        ArrayList<String> strategyColumns = new ArrayList<>();
        if (columns == null )
            return strategyColumns;
        for (String column : columns.keySet()) {
            String strategy = columns.get(column).getStrategy();
            if (strategy != null)
                strategyColumns.add(column);
        }
        return strategyColumns;
    }


    @Override
    public boolean isArchiveTags() {
        return archiveTags;
    }

    public void setArchiveTags(boolean archiveTags) {
        this.archiveTags = archiveTags;
    }

    @Override
    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public void setStaticDataClazz(String staticDataClazz) {
        this.staticDataClazz = staticDataClazz;
    }

    public String getStaticDataClazz() {
        return staticDataClazz;
    }
}
