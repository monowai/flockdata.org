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

import com.auditbucket.client.csv.CsvTag;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.TagInputBean;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;

/**
 * User: mike
 * Date: 27/08/14
 * Time: 7:53 AM
 */
public class CsvHelper {
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(CsvHelper.class);

    public static TagInputBean setNestedTags(TagInputBean setInTo, ArrayList<CsvTag> tagsToAnalyse, Map<String, Object> csvRow) throws DatagioException {
        if (tagsToAnalyse == null)
            return null;

        TagInputBean newTag = null;

        for (CsvTag csvTag : tagsToAnalyse) {
            Object value = csvRow.get(csvTag.getColumn());
            if ( value == null ){
                logger.error("Undefined row value for {}", csvTag.getColumn());
                throw new DatagioException(String.format("Undefined row value for %s", csvTag.getColumn()));
            }

            newTag = new TagInputBean(value.toString())
                    .setLabel(csvTag.getLabel());
            newTag.setReverse(csvTag.getReverse());
            newTag.setMustExist(csvTag.getMustExist());
            setInTo.setTargets(csvTag.getRelationship(), newTag);
            if (csvTag.getTargets() != null) {
                setNestedTags(newTag, csvTag.getTargets(), csvRow);
            }

        }
        return newTag;
    }

}
