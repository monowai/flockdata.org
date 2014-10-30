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
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.FdReader;
import org.flockdata.transform.TagProfile;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * User: mike
 * Date: 27/08/14
 * Time: 7:53 AM
 */
public class CsvHelper {
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(CsvHelper.class);

    public static boolean getTagInputBean(TagInputBean tag,
                                          FdReader staticDataResolver,
                                          Map<String, Object> row,
                                          String column,
                                          ColumnDefinition colDef,
                                          String value) throws FlockException {

        if (colDef.isCountry()) {
            value = staticDataResolver.resolveCountryISOFromName(value);
        }
        Map<String, Object> properties = new HashMap<>();

        if (colDef.isValueAsProperty()) {
            tag.setMustExist(colDef.isMustExist()).setLabel(column);
            tag.setReverse(colDef.getReverse());
            tag.setName(column);
            tag.setCode(column);
            if (Integer.decode(value) != 0) {
                properties.put("value", Integer.decode(value));
                if (colDef.getNameColumn() != null) {
                    tag.addEntityLink(row.get(colDef.getNameColumn()).toString(), properties);
                } else if (colDef.getRelationshipName() != null) {
                    tag.addEntityLink(colDef.getRelationshipName(), properties);
                } else
                    tag.addEntityLink("undefined", properties);
            } else {
                return false;
            }
        } else {
            if (value == null || value.equals("") ){
                // Value is missing in the data set - see if there is a default
                value = colDef.getNullOrEmpty();
            }
            String label = (colDef.getLabel()!=null? colDef.getLabel(): column);
            if (colDef.getCode() != null)
                tag.setCode(row.get(colDef.getCode()).toString());
            else
                tag.setCode(value);

            tag.setName(value).setMustExist(colDef.isMustExist()).setLabel(colDef.isCountry() ? "Country" : label);
            tag.addEntityLink(colDef.getRelationshipName());
            tag.setReverse(colDef.getReverse());
        }
        setNestedTags(tag, colDef.getTargets(), row,staticDataResolver);
        return true;
    }

    public static TagInputBean setNestedTags(TagInputBean setInTo, ArrayList<TagProfile> tagsToAnalyse, Map<String, Object> csvRow, FdReader reader) throws FlockException {
        if (tagsToAnalyse == null)
            return null;

        TagInputBean newTag = null;

        for (TagProfile tagProfile : tagsToAnalyse) {
            Object value = csvRow.get(tagProfile.getColumn());

            if ( value == null ){
                logger.error("Undefined row value for {}", tagProfile.getColumn());
                throw new FlockException(String.format("Undefined row value for %s", tagProfile.getColumn()));
            }

            if ( tagProfile.getDelimiter() != null ){
                // No known entity relationship
                setInTo.setTargets(tagProfile.getRelationship(), getTagsFromList(tagProfile, csvRow,null));
            } else if ( tagProfile.isCountry()) {
                String iso = reader.resolveCountryISOFromName(value.toString());
                if ( iso == null  ) // Regression tests
                    iso = value.toString();
                newTag = new TagInputBean(iso)
                        .setLabel(tagProfile.getLabel());
                setInTo.setTargets(tagProfile.getRelationship(), newTag);

            }else{
                newTag = new TagInputBean(value.toString())
                        .setLabel(tagProfile.getLabel());
                newTag.setReverse(tagProfile.getReverse());
                newTag.setMustExist(tagProfile.getMustExist());
                setInTo.setTargets(tagProfile.getRelationship(), newTag);
            }
            if (tagProfile.getTargets() != null) {
                setNestedTags(newTag, tagProfile.getTargets(), csvRow, reader);
            }

        }
        return newTag;
    }

    public static Collection<TagInputBean>getTagsFromList(TagProfile tagProfile, Map<String, Object> row, String entityRelationship ){
        List<String> tags = Arrays.asList(row.get(tagProfile.getColumn()).toString().split(tagProfile.getDelimiter()));
        Collection<TagInputBean>results = new ArrayList<>();

        for (String tag : tags) {
            TagInputBean newTag = new TagInputBean(tag,entityRelationship)
                    .setLabel(tagProfile.getLabel());
            newTag.setReverse(tagProfile.getReverse());
            newTag.setMustExist(tagProfile.getMustExist());
            newTag.setLabel(tagProfile.getLabel());
            results.add(newTag);
        }
        return results;
    }

    public static Map<String, Object> convertToMap(String[] headerRow, String[] line) {
        int col = 0;
        Map<String, Object> row = new HashMap<>();
        for (String column : headerRow) {
            row.put(column, line[col]);
            col++;
        }
        return row;
    }


}
