package com.auditbucket.client.common;

import com.auditbucket.client.Importer;
import com.auditbucket.client.csv.CsvColumnDefinition;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * User: mike
 * Date: 9/05/14
 * Time: 8:45 AM
 */
public class ImportParamsDeserializer extends JsonDeserializer<ImportParams> {
    @Override
    public ImportParams deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ImportParams importParams = new ImportParams();
        JsonNode node = jp.getCodec().readTree(jp);
        JsonNode column = node.get("documentType");
        if (column != null)
            importParams.setDocumentType(column.asText());

        column = node.get("clazz");
        if (column != null)
            importParams.setClazz(column.asText());

        column = node.get("fortress");
        if (column != null)
            importParams.setFortress(column.get("name").asText());

        column = node.get("tagOrTrack");
        if (column != null)
            importParams.setTagOrTrack(column.asText());

        column = node.get("fortressUser");
        if (column != null)
            importParams.setFortressUser(column.asText());

        column = node.get("staticDataClazz");
        if (column != null)
            importParams.setStaticDataClazz(column.asText());


        column = node.get("metaOnly");
        if (column != null)
            importParams.setMetaOnly(Boolean.parseBoolean(column.asText()));

        column = node.get("header");
        if (column != null)
            importParams.setHeader(Boolean.parseBoolean(column.asText()));

        column = node.get("delimiter");
        if (column != null)
            importParams.setDelimiter(toString().charAt(0));

        column = node.get("metaHeader");
        if ( column!=null) {
            importParams.setMetaHeader(column.asText());
        }


        column = node.get("importType");
        if (column != null) {
            switch (column.textValue().toLowerCase()) {
                case "csv":
                    importParams.setImportType(Importer.importer.CSV);
                    break;
                case "xml":
                    importParams.setImportType(Importer.importer.XML);
                    break;
                case "json":
                    importParams.setImportType(Importer.importer.JSON);
                    break;
            }
        }
        column = node.get("csvHeaders");
        if ( column !=null ){
            ObjectMapper mapper = new ObjectMapper();
            Iterator<Map.Entry<String,JsonNode>> columns = column.fields();
            Map<String,CsvColumnDefinition>csvHeaders = new HashMap<>();
            while (columns.hasNext()) {
                Map.Entry<String, JsonNode> next = columns.next();
                String colName = next.getKey();
                CsvColumnDefinition columnDefinition = mapper.readValue(next.getValue().toString(), CsvColumnDefinition.class);
                csvHeaders.put(colName, columnDefinition);
            }
            importParams.setCsvHeaders(csvHeaders);
        }
        return importParams;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
