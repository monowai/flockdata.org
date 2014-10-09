package com.auditbucket.transform.csv;

import com.auditbucket.helper.FlockException;
import com.auditbucket.profile.model.ProfileConfiguration;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.model.EntityKey;
import com.auditbucket.transform.ColumnDefinition;
import com.auditbucket.transform.DelimitedMappable;
import com.auditbucket.transform.FdReader;
import com.auditbucket.transform.TagProfile;
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

    public CsvEntityMapper(com.auditbucket.profile.ImportProfile importProfile) {
        setDocumentType(importProfile.getDocumentType());
        setFortress(importProfile.getFortress());
        setFortressUser(importProfile.getFortressUser());
    }

    @Override
    public ProfileConfiguration.ContentType getImporter() {
        return ProfileConfiguration.ContentType.CSV;
    }

    @Override
    public ProfileConfiguration.DataType getABType() {
        return ProfileConfiguration.DataType.TRACK;
    }

    private Map<String, Object> toMap(ProfileConfiguration importProfile, String[] headerRow, String[] line) {
        int col = 0;

        Map<String, Object> row = new HashMap<>();
        for (String column : headerRow) {
            ColumnDefinition colDef = importProfile.getColumnDef(column);

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
    public Map<String, Object> setData(final String[] headerRow, final String[] line, ProfileConfiguration importProfile, FdReader dataResolver) throws JsonProcessingException, FlockException {
        int col = 0;
        Map<String, Object> row = toMap(importProfile, headerRow, line);
        setArchiveTags(importProfile.isArchiveTags());

        for (String column : headerRow) {
            column = column.trim();
            ColumnDefinition colDef = importProfile.getColumnDef(column);

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
                        TagProfile tagProfile = new TagProfile();
                        tagProfile.setLabel(colDef.getLabel());
                        tagProfile.setReverse(colDef.getReverse());
                        tagProfile.setMustExist(colDef.isMustExist());
                        tagProfile.setColumn(column);
                        tagProfile.setDelimiter(colDef.getDelimiter());
                        Collection<TagInputBean> tags = CsvHelper.getTagsFromList(tagProfile, row, colDef.getRelationshipName());
                        for (TagInputBean tag : tags) {
                            addTag(tag);
                        }

                    }
                } else if (colDef.isTag()) {
                    TagInputBean tag = new TagInputBean();
                    if (CsvHelper.getTagInputBean(tag, dataResolver, row, column, colDef, value))
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
        Collection<String> strategyCols = importProfile.getStrategyCols();
        for (String strategyCol : strategyCols) {
            ColumnDefinition colDef = importProfile.getColumnDef(strategyCol);
            String callerRef = dataResolver.resolve(strategyCol, getColumnValues(colDef, row));

            if (callerRef != null) {
                addCrossReference(colDef.getStrategy(), new EntityKey(colDef.getFortress(), colDef.getDocumentType(), callerRef));
            }
        }

        if (importProfile.getEntityKey() != null) {
            ColumnDefinition columnDefinition = importProfile.getColumnDef(importProfile.getEntityKey());
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



    private Map<String, Object> getColumnValues(ColumnDefinition colDef, Map<String, Object> row) {
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

    public static DelimitedMappable newInstance(com.auditbucket.profile.ImportProfile importProfile) {
        return new CsvEntityMapper(importProfile);
    }

    @Override
    public char getDelimiter() {
        return ',';
    }

}
