package com.auditbucket.client;

import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * User: mike
 * Date: 28/04/14
 * Time: 8:47 AM
 */
public class ImportParams {

    private String documentType;
    private Importer.importer importType;
    private String tagOrTrack;
    private String clazz = null;
    private char delimiter = ',';
    private String fortress = null;
    private boolean header = true;
    private AbRestClient restClient;
    private String fortressUser;
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(ImportParams.class);

    public ImportParams() {

    }

    public ImportParams(String clazz, AbRestClient restClient) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        this.clazz = clazz;
        this.restClient = restClient;
        this.importType = ((Mappable) Class.forName(getClazz()).newInstance()).getImporter();

    }

    @Override
    public String toString() {
        return "ImportParams{" +
                "documentType='" + documentType + '\'' +
                ", importType=" + importType +
                ", tagOrTrack='" + tagOrTrack + '\'' +
                ", clazz='" + clazz + '\'' +
                ", delimiter=" + delimiter +
                '}';
    }

    public ImportParams(Map params, AbRestClient restClient) {
        this();
        this.restClient = restClient;
        Object o = params.get("documentType");
        if (o != null)
            documentType = o.toString();

        o = params.get("clazz");
        if (o != null)
            clazz = o.toString();

        o = params.get("fortress");
        if (o != null) {
            Map f = (Map) o;
            fortress = f.get("name").toString();

        }

        o = params.get("tagOrTrack");
        if (o != null)
            tagOrTrack = o.toString();

        o = params.get("fortressUser");
        if (o != null)
            fortressUser = o.toString();


        o = params.get("header");
        if (o != null)
            header = Boolean.parseBoolean(o.toString());

        o = params.get("delimiter");
        if (o != null)
            delimiter = o.toString().charAt(0);

        o = params.get("importType");
        if (o != null) {
            switch (o.toString().toLowerCase()) {
                case "csv":
                    importType = Importer.importer.CSV;
                    break;
                case "xml":
                    importType = Importer.importer.XML;
                    break;
                case "json":
                    importType = Importer.importer.JSON;
                    break;


            }
        }
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

    public String getTagOrTrack() {
        return tagOrTrack;
    }

    public void setTagOrTrack(String tagOrTrack) {
        this.tagOrTrack = tagOrTrack;
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

        if ( !(clazz == null || clazz.equals("")))
            mappable = (Mappable) Class.forName(getClazz()).newInstance();
        else if (getTagOrTrack().equalsIgnoreCase("track")) {
            mappable = TrackMapper.newInstance(this);
        } else if (getTagOrTrack().equalsIgnoreCase("tag")) {
            mappable = TagMapper.newInstance(this);
        } else
            logger.error ("Unable to determine the implementing handler");


        return mappable;

    }

    public AbRestClient getRestClient() {
        return restClient;
    }

    public StaticDataResolver getStaticDataResolver() {
        return restClient;
    }

    public boolean isSimulateOnly() {
        return restClient.isSimulateOnly();
    }

    public String getFortressUser() {
        return fortressUser;
    }
}
