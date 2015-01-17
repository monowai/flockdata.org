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

package org.flockdata.transform.csv;

import org.flockdata.helper.FlockException;
import org.flockdata.profile.ImportProfile;
import org.flockdata.profile.model.ProfileConfiguration;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.model.EntityKey;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.DelimitedMappable;
import org.flockdata.transform.FdReader;
import org.flockdata.transform.TagProfile;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.math.NumberUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * User: mike
 * Date: 27/04/14
 * Time: 4:34 PM
 */
public class CsvEntityMapper extends EntityInputBean implements DelimitedMappable {

    private Logger logger = LoggerFactory.getLogger(CsvEntityMapper.class);

    public CsvEntityMapper(ImportProfile importProfile) {
        setDocumentType(importProfile.getDocumentName());
        setFortress(importProfile.getFortressName());
        setFortressUser(importProfile.getFortressUser());
    }

    @Override
    public ProfileConfiguration.ContentType getImporter() {
        return ProfileConfiguration.ContentType.CSV;
    }

    private Map<String, Object> toMap(ProfileConfiguration importProfile, String[] headerRow, String[] line) {
        int col = 0;

        Map<String, Object> row = new HashMap<>();
        for (String column : headerRow) {
            ColumnDefinition colDef = importProfile.getColumnDef(column);
            if ( line[col]==null || line[col].equals("null"))
                row.put(column, null);
            else if (NumberUtils.isNumber(line[col])) {
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
        Map<String, Object> row = toMap(importProfile, headerRow, line);
        setArchiveTags(importProfile.isArchiveTags());

        for (String column : headerRow) {
            column = column.trim();
            ColumnDefinition colDef = importProfile.getColumnDef(column);

            if (colDef != null) {
                Object o = row.get(column);
                String value = null ;
                if ( o !=null )
                    value = o.toString().trim();

                if (colDef.isDescription()) {
                    setDescription(row.get(column).toString());
                }
                if ( colDef.isCreateDate()){
                    if ( colDef.isDateEpoc()) {
                        long val = Long.parseLong(value)*1000;

                        setWhen(new DateTime(val));
//                        logger.debug ("{}, {}, {}", val, value, getWhen());
                    }
                    else if ( NumberUtils.isDigits(value))  // plain old java millis
                        setWhen(new DateTime(Long.parseLong(value)));
                    else // ToDo: apply a date format
                        setWhen( new DateTime(value));

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
                    if (CsvHelper.getTagInputBean(tag, dataResolver, row, column, importProfile.getContent(), value))
                        addTag(tag);
                }
                if (colDef.isTitle()) {
                    setName(value);
                }
                if (colDef.isCreateUser()) { // The user in the calling system
                    setFortressUser(value);
                }

                if (colDef.isUpdateUser()) {
                    setUpdateUser(value);
                }
                if ( !colDef.getCrossReferences().isEmpty()){
                    for (Map<String, String> key : colDef.getCrossReferences()) {
                        addCrossReference(key.get("relationshipName"), new EntityKey(key.get("fortress"), key.get("documentName"), value));
                    }
                }

            } // ignoreMe
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
                String[] dataCols = columnDefinition.getRefColumns();
                String callerRef = "";
                for (String dataCol : dataCols) {
                    callerRef = callerRef + (!callerRef.equals("") ? "." : "") + row.get(dataCol);
                }
                setCallerRef(callerRef);
            }

        }

        return row;
    }



    private Map<String, Object> getColumnValues(ColumnDefinition colDef, Map<String, Object> row) {
        Map<String, Object> results = new HashMap<>();
        String[] columns = colDef.getRefColumns();
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

    public static DelimitedMappable newInstance(ImportProfile importProfile) {
        return new CsvEntityMapper(importProfile);
    }

    @Override
    public char getDelimiter() {
        return ',';
    }

}
