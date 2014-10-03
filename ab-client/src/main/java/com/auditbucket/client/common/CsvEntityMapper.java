package com.auditbucket.client.common;

import com.auditbucket.client.Importer;
import com.auditbucket.client.csv.CsvColumnDefinition;
import com.auditbucket.client.csv.CsvTag;
import com.auditbucket.client.rest.AbRestClient;
import com.auditbucket.helper.FlockException;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.model.EntityKey;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * User: mike
 * Date: 27/04/14
 * Time: 4:34 PM
 */
public class CsvEntityMapper extends EntityInputBean implements DelimitedMappable {
    //private static org.slf4j.Logger logger = LoggerFactory.getLogger(CsvTrackMapper.class);

    public CsvEntityMapper(ImportParams importParams) {
        setDocumentType(importParams.getDocumentType());
        setFortress(importParams.getFortress());
        setFortressUser(importParams.getFortressUser());
    }

    @Override
    public Importer.importer getImporter() {
        return Importer.importer.CSV;
    }

    @Override
    public AbRestClient.type getABType() {
        return AbRestClient.type.TRACK;
    }

    private Map<String, Object> toMap(ImportParams importParams, String[] headerRow, String[] line) {
        int col = 0;

        Map<String, Object> row = new HashMap<>();
        for (String column : headerRow) {
            CsvColumnDefinition colDef = importParams.getColumnDef(column);

            if (NumberUtils.isNumber(line[col])) {
                if ( colDef !=null &&  colDef.getType()!=null && colDef.getType().equalsIgnoreCase("string"))
                    row.put(column.trim(), String.valueOf(line[col]));
                else
                    row.put(column.trim(), NumberUtils.createNumber(line[col]));
            } else {
//                Date date = null;
//                try {
//                    if ( colDef!=null && colDef.getDateFormat()!=null ) {
//                        date = DateUtils.parseDate(line[col], colDef.getDateFormat());
//                        row.put(column, date.getTime());
//                    }
//                } catch (ParseException e) {
//                    //
//                }
                //if ( date == null ) // Stash it as a string
                row.put(column.trim(), (line[col]==null ? null :line[col].trim()));
            }

            col++;
        }
        return row;
    }

    @Override
    public Map<String, Object> setData(final String[] headerRow, final String[] line, ImportParams importParams) throws JsonProcessingException, FlockException {
        int col = 0;
        Map<String, Object> row = toMap(importParams, headerRow, line);
        setArchiveTags(importParams.isArchiveTags());

        for (String column : headerRow) {
            column = column.trim();
            CsvColumnDefinition colDef = importParams.getColumnDef(column);

            if (colDef != null) {
                String value = line[col];
                if (value != null)
                    value = value.trim();

                if (colDef.isDescription()) {
                    setDescription(row.get(column).toString());
                }
                if (colDef.isCallerRef()) {
                    String callerRef = getCallerRef();
                    if (callerRef == null)
                        callerRef = value;
                    else
                        callerRef = callerRef + "." + value;

                    setCallerRef(callerRef);
                }
                if (colDef.getDelimiter() != null) {
                    // Implies a tag because it is a comma delimited list of values
                    if (value != null && !value.equals("")) {
                        CsvTag csvTag = new CsvTag();
                        csvTag.setLabel(colDef.getLabel());
                        csvTag.setReverse(colDef.getReverse());
                        csvTag.setMustExist(colDef.isMustExist());
                        csvTag.setColumn(column);
                        csvTag.setDelimiter(colDef.getDelimiter());
                        Collection<TagInputBean> tags = CsvHelper.getTagsFromList(csvTag, row, colDef.getRelationshipName());
                        for (TagInputBean tag : tags) {
                            addTag(tag);
                        }

                    }
                } else if (colDef.isTag()) {
                    TagInputBean tag = new TagInputBean();
                    if (CsvHelper.getTagInputBean(tag, importParams, row, column, colDef, value))
                        addTag(tag);
                }
                if (colDef.isTitle()) {
                    setName(value);
                }
                if (colDef.isCreatedUser()) { // The user in the calling system
                    setFortressUser(value);
                }

                if (colDef.isUpdateUser())
                    setUpdateUser(value);

            } // ignoreMe
            col++;
        }
        Collection<String> strategyCols = importParams.getStrategyCols();
        for (String strategyCol : strategyCols) {
            CsvColumnDefinition colDef = importParams.getColumnDef(strategyCol);
            String callerRef = importParams.getStaticDataResolver().resolve(strategyCol, getColumnValues(colDef, row));

            if (callerRef != null) {
                addCrossReference(colDef.getStrategy(), new EntityKey(colDef.getFortress(), colDef.getDocumentType(), callerRef));
            }
        }

        if (importParams.getEntityKey() != null) {
            CsvColumnDefinition columnDefinition = importParams.getColumnDef(importParams.getEntityKey());
            if (columnDefinition != null) {
                String[] metaCols = columnDefinition.getRefColumns();
                String callerRef = "";
                for (String metaCol : metaCols) {
                    callerRef = callerRef + (!callerRef.equals("") ? "." : "") + row.get(metaCol);
                }
                setCallerRef(callerRef);
            }

        }

        return row;
    }



    private Map<String, Object> getColumnValues(CsvColumnDefinition colDef, Map<String, Object> row) {
        Map<String, Object> results = new HashMap<>();
        String[] columns = colDef.getColumns();

        int i = 0;
        int max = columns.length;
        while (i < max) {
            results.put(columns[i], row.get(columns[i]));
            i++;
        }
        return results;
    }

    @Override
    public boolean hasHeader() {
        return true;
    }

    public static DelimitedMappable newInstance(ImportParams importParams) {
        return new CsvEntityMapper(importParams);
    }

    @Override
    public char getDelimiter() {
        return ',';
    }

}
