/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.client.common;

import com.auditbucket.client.csv.CsvColumnDefinition;
import com.auditbucket.client.csv.CsvTag;
import com.auditbucket.helper.FlockException;
import com.auditbucket.registration.bean.TagInputBean;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * User: mike
 * Date: 27/08/14
 * Time: 7:53 AM
 */
public class CsvHelper {
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(CsvHelper.class);

    public static boolean getTagInputBean(TagInputBean tag, ImportParams importParams, Map<String, Object> row, String column, CsvColumnDefinition colDef, String value) throws FlockException {

        if (colDef.isCountry()) {
            value = importParams.getStaticDataResolver().resolveCountryISOFromName(value);
        }
        Map<String, Object> properties = new HashMap<>();
//        if ( colDef.getDelimiter() != null ){
//
//            tag.setTargets(colDef.getRelationshipName(), getTagsFromList(csvTag,row));
//        }
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
            String label = (colDef.getLabel()!=null? colDef.getLabel(): column);
            if (colDef.getCode() != null)
                tag.setCode(row.get(colDef.getCode()).toString());
            else
                tag.setCode(value);

            tag.setName(value).setMustExist(colDef.isMustExist()).setLabel(colDef.isCountry() ? "Country" : label);
            tag.addEntityLink(colDef.getRelationshipName());
            tag.setReverse(colDef.getReverse());
        }
        setNestedTags(tag, colDef.getTargets(), row);
        return true;
    }

    public static TagInputBean setNestedTags(TagInputBean setInTo, ArrayList<CsvTag> tagsToAnalyse, Map<String, Object> csvRow) throws FlockException {
        if (tagsToAnalyse == null)
            return null;

        TagInputBean newTag = null;

        for (CsvTag csvTag : tagsToAnalyse) {
            Object value = csvRow.get(csvTag.getColumn());

            if ( value == null ){
                logger.error("Undefined row value for {}", csvTag.getColumn());
                throw new FlockException(String.format("Undefined row value for %s", csvTag.getColumn()));
            }

            if ( csvTag.getDelimter() != null ){
                // No known entity relationship
                setInTo.setTargets(csvTag.getRelationship(), getTagsFromList(csvTag, csvRow,null));
            } else {
                newTag = new TagInputBean(value.toString())
                        .setLabel(csvTag.getLabel());
                newTag.setReverse(csvTag.getReverse());
                newTag.setMustExist(csvTag.getMustExist());
                setInTo.setTargets(csvTag.getRelationship(), newTag);
            }
            if (csvTag.getTargets() != null) {
                setNestedTags(newTag, csvTag.getTargets(), csvRow);
            }

        }
        return newTag;
    }

    public static Collection<TagInputBean>getTagsFromList(CsvTag csvTag, Map<String, Object> row, String entityRelationship ){
        List<String> tags = Arrays.asList(row.get(csvTag.getColumn()).toString().split(csvTag.getDelimter()));
        Collection<TagInputBean>results = new ArrayList<>();

        for (String tag : tags) {
            TagInputBean newTag = new TagInputBean(tag,entityRelationship)
                    .setLabel(csvTag.getLabel());
            newTag.setReverse(csvTag.getReverse());
            newTag.setMustExist(csvTag.getMustExist());
            newTag.setLabel(csvTag.getLabel());
            results.add(newTag);
        }
        return results;
    }

}
