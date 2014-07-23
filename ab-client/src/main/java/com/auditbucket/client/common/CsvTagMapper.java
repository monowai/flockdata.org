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

import com.auditbucket.client.Importer;
import com.auditbucket.client.csv.CsvColumnHelper;
import com.auditbucket.client.csv.CsvTag;
import com.auditbucket.client.rest.AbRestClient;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.TagInputBean;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * User: mike
 * Date: 27/04/14
 * Time: 4:34 PM
 */
public class CsvTagMapper extends TagInputBean implements DelimitedMappable {
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(CsvTagMapper.class);

    public CsvTagMapper(ImportParams importParams) {
    }

    @Override
    public Importer.importer getImporter() {
        return Importer.importer.CSV;
    }

    @Override
    public AbRestClient.type getABType() {
        return AbRestClient.type.TAG;
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
    public String setData(final String[] headerRow, final String[] line, ImportParams importParams) throws JsonProcessingException, DatagioException {
        int col = 0;
        Map<String, Object> row = toMap(headerRow, line);

        for (String column : headerRow) {
            CsvColumnHelper columnHelper = new CsvColumnHelper(column, line[col], importParams.getColumnDef(headerRow[col]));
            if (!columnHelper.ignoreMe()) {
                //headerRow[col] = columnHelper.getKey();
                if (columnHelper.isTag()) {

                    String val = columnHelper.getValue();
                    if (val != null && !val.equals("")) {
                        val = columnHelper.getValue();
                        if (columnHelper.isCountry()) {
                            val = importParams.getStaticDataResolver().resolveCountryISOFromName(val);
                        }
                        setName(val);
                        setCode(val);
                        String index = columnHelper.getKey();
                        setMustExist(columnHelper.isMustExist()).setIndex(columnHelper.isCountry() ? "Country" : index);

                        ArrayList<CsvTag> targets = columnHelper.getColumnDefinition().getTargets();
                        for (CsvTag target : targets) {
                            Object tagName = row.get(target.getColumn());
                            if (tagName == null) {
                                logger.error("No 'column' value found for {} in the {} entry ", target.getColumn(), column);
                            } else {
                                TagInputBean targetTag = new TagInputBean(tagName.toString())
                                        .setIndex(target.getIndex());
                                targetTag.setReverse(target.getReverse());
                                setTargets(target.getRelationship(), targetTag);
                            }
                        }

                    }
                }
                if (columnHelper.isTitle()) {
                    setName(line[col]);
                }
            } // ignoreMe
            col++;
        }
        return AbRestClient.convertToJson(headerRow, line);
    }

    @Override
    public boolean hasHeader() {
        return true;
    }

    public static DelimitedMappable newInstance(ImportParams importParams) {
        return new CsvTagMapper(importParams);
    }

    @Override
    public char getDelimiter() {
        return ',';
    }
}
