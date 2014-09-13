package com.auditbucket.client.common;

import com.auditbucket.client.Importer;
import com.auditbucket.client.csv.CsvColumnDefinition;
import com.auditbucket.client.rest.AbRestClient;
import com.auditbucket.client.rest.IStaticDataResolver;
import com.auditbucket.client.rest.StaticDataResolver;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * See also ImportParamsDeserializer for mapping logic
 * User: mike
 * Date: 28/04/14
 * Time: 8:47 AM
 */
@JsonDeserialize(using = ImportParamsDeserializer.class)
public class ImportParams {

    private String documentType;
    private Importer.importer importType;
    private String tagOrEntity;
    private String clazz = null;
    private String staticDataClazz;
    private char delimiter = ',';
    private String fortress = null;
    private boolean header = true;
    private AbRestClient restClient;
    private String fortressUser;
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(ImportParams.class);
    private boolean entityOnly;

    private Map<String, CsvColumnDefinition> csvHeaders;
    private IStaticDataResolver staticDataResolver;
    private String entityKey;

    public ImportParams() {

    }

    public ImportParams(AbRestClient restClient) {
        this.restClient = restClient;
    }

    public ImportParams(String clazz, AbRestClient restClient) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        this(restClient);
        this.clazz = clazz;
        this.importType = ((Mappable) Class.forName(getClazz()).newInstance()).getImporter();

    }

    public void setHeader(boolean header) {
        this.header = header;
    }

    public void setRestClient(AbRestClient restClient) {
        this.restClient = restClient;
    }

    public void setFortressUser(String fortressUser) {
        this.fortressUser = fortressUser;
    }

    public void setCsvHeaders(Map<String, CsvColumnDefinition> csvHeaders) {
        this.csvHeaders = csvHeaders;
    }

    @Override
    public String toString() {
        return "ImportParams{" +
                "documentType='" + documentType + '\'' +
                ", importType=" + importType +
                ", tagOrEntity='" + tagOrEntity + '\'' +
                ", clazz='" + clazz + '\'' +
                ", delimiter=" + delimiter +
                '}';
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public Importer.importer getImportType() {
        return importType;
    }

    public void setImportType(Importer.importer importType) {
        this.importType = importType;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public char getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(char delimiter) {
        this.delimiter = delimiter;
    }

    public String getTagOrEntity() {
        return tagOrEntity;
    }

    public void setTagOrEntity(String tagOrEntity) {
        if ( tagOrEntity.equalsIgnoreCase("track")) // Backward compatibility. We should look to remove.
            tagOrEntity = "entity";
        this.tagOrEntity = tagOrEntity;
    }

    public String getFortress() {
        return fortress;
    }

    public void setFortress(String fortress) {
        this.fortress = fortress;
    }

    public boolean hasHeader() {
        return header;
    }

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

    public AbRestClient getRestClient() {
        return restClient;
    }

    public boolean isSimulateOnly() {
        return restClient.isSimulateOnly();
    }

    public String getFortressUser() {
        return fortressUser;
    }

    public boolean isEntityOnly() {
        return entityOnly;
    }

    public void setEntityOnly(boolean entityOnly) {
        this.entityOnly = entityOnly;
    }

    public CsvColumnDefinition getColumnDef(String header) {
        if (csvHeaders == null)
            return null;
        return csvHeaders.get(header);
    }

    public void setStaticDataResolver(IStaticDataResolver staticDataResolver) {
        this.staticDataResolver = staticDataResolver;
    }

    public IStaticDataResolver getStaticDataResolver() {
        if (staticDataResolver != null)
            return staticDataResolver;

        if (staticDataClazz == null)
            return new StaticDataResolver(restClient);
        else {
            try {
                Constructor<StaticDataResolver> constructor = (Constructor<StaticDataResolver>) Class.forName(staticDataClazz).getConstructor(AbRestClient.class);
                staticDataResolver = constructor.newInstance(restClient);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                logger.error("Unexpected", e);
            }
            return staticDataResolver;// Unit testing
        }
    }

    public void setEntityKey(String entityKey) {
        this.entityKey = entityKey;
    }

    public String getEntityKey() {
        return entityKey;
    }

    public Map<String, CsvColumnDefinition> getColumns() {
        return csvHeaders;
    }

    public Collection<String> getStrategyCols() {
        Map<String, CsvColumnDefinition> columns = getColumns();
        ArrayList<String> strategyColumns = new ArrayList<>();
        for (String column : columns.keySet()) {
            String strategy = columns.get(column).getStrategy();
            if (strategy != null)
                strategyColumns.add(column);
        }
        return strategyColumns;
    }


    public void setStaticDataClazz(String staticDataClazz) {
        this.staticDataClazz = staticDataClazz;
    }
}
