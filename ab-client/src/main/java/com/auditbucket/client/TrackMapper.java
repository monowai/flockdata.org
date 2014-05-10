package com.auditbucket.client;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.HashMap;
import java.util.Map;

/**
 * User: mike
 * Date: 27/04/14
 * Time: 4:34 PM
 */
public class TrackMapper extends MetaInputBean implements DelimitedMappable {
    public TrackMapper(ImportParams importParams) {
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

    private Map<String, Object> toMap(String[] headerRow, String[] line) {
        int col = 0;
        Map<String, Object> row = new HashMap<>();
        for (String column : headerRow) {
            row.put(column, line[col]);
            col++;
        }
        return row;
    }

    @Override
    public String setData(String[] headerRow, String[] line, ImportParams importParams) throws JsonProcessingException, DatagioException {
        int col = 0;
        Map<String, Object> row = toMap(headerRow, line);

        for (String column : headerRow) {
            CsvColumnHelper columnHelper = new CsvColumnHelper(column, line[col], importParams.getColumnDef(headerRow[col]));
            if (!columnHelper.ignoreMe()) {
                headerRow[col] = columnHelper.getKey();
                if (columnHelper.isCallerRef()) {
                    String callerRef = getCallerRef();
                    if (callerRef == null)
                        callerRef = columnHelper.getValue();
                    else
                        callerRef = callerRef + "." + columnHelper.getValue();

                    setCallerRef(callerRef);
                } else if (columnHelper.isTag()) {
                    String thisColumn = columnHelper.getKey();

                    String val = columnHelper.getValue();
                    if (val != null && !val.equals("")) {
                        TagInputBean tag;
                        val = columnHelper.getValue();
                        if (columnHelper.isCountry()) {
                            val = importParams.getStaticDataResolver().resolveCountryISOFromName(val);
                        }
                        Map<String, Object> properties = new HashMap<>();
                        if (columnHelper.isValueAsProperty()) {
                            tag = new TagInputBean(thisColumn).setMustExist(columnHelper.isMustExist()).setIndex(thisColumn);

                            if (Integer.decode(columnHelper.getValue()) != 0) {
                                properties.put("value", Integer.decode(columnHelper.getValue()));
                                if (columnHelper.getNameColumn() != null) {
                                    tag.addMetaLink(row.get(columnHelper.getNameColumn()).toString(), properties);
                                } else if (columnHelper.getRelationshipName() != null) {
                                    tag.addMetaLink(columnHelper.getRelationshipName(), properties);
                                } else
                                    tag.addMetaLink("undefined", properties);
                            } else {
                                break; // Don't set a 0 value tag
                            }
                        } else {
                            // Assume column of "Specialist" and value of "Orthopedic"
                            // Index == Specialist and Type = Orthopedic
                            String index = columnHelper.getKey();

                            tag = new TagInputBean(val).setMustExist(columnHelper.isMustExist()).setIndex(columnHelper.isCountry() ? "Country" : index);
                            tag.addMetaLink(columnHelper.getRelationshipName());
                        }

                        setTag(tag);
                    }
                }
                if (columnHelper.isTitle()) {
                    setName(line[col]);
                }
            } // ignoreMe
            col++;
        }
        if (importParams.getMetaHeader() != null) {
            CsvColumnDefinition columnDefinition = importParams.getColumnDef(importParams.getMetaHeader());
            if (columnDefinition != null) {
                String[] metaCols = columnDefinition.getRefColumns();
                String callerRef = "";
                for (String metaCol : metaCols) {
                    callerRef = callerRef + (!callerRef.equals("") ? "." : "") + row.get(metaCol);
                }
                setCallerRef(callerRef);
            }

        }
        return AbRestClient.convertToJson(headerRow, line);
    }

    @Override
    public boolean hasHeader() {
        return true;
    }

    public static DelimitedMappable newInstance(ImportParams importParams) {
        return new TrackMapper(importParams);
    }

    @Override
    public char getDelimiter() {
        return ',';
    }
}
