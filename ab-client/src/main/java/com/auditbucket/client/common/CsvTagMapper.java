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
import com.auditbucket.client.csv.CsvColumnDefinition;
import com.auditbucket.client.rest.AbRestClient;
import com.auditbucket.helper.FlockException;
import com.auditbucket.registration.bean.TagInputBean;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Map;

/**
 * User: mike
 * Date: 27/04/14
 * Time: 4:34 PM
 */
public class CsvTagMapper extends TagInputBean implements DelimitedMappable {


    @Override
    public Importer.importer getImporter() {
        return Importer.importer.CSV;
    }

    @Override
    public AbRestClient.type getABType() {
        return AbRestClient.type.TAG;
    }

    @Override
    public Map<String, Object> setData(final String[] headerRow, final String[] line, ImportParams importParams) throws JsonProcessingException, FlockException {
        int col = 0;
        Map<String, Object> row = AbRestClient.convertToMap(headerRow, line);

        for (String column : headerRow) {
            CsvColumnDefinition colDef = importParams.getColumnDef(column);
            String value = line[col];
            if ( value !=null )
                value = value.trim();

            if (colDef!=null) {

                if (colDef.isTag()) {
                    if (value != null && !value.equals("")) {
                        CsvHelper.getTagInputBean(this, importParams, row, column, colDef, value);
                    }
                }
                if (colDef.isTitle()) {
                    setName(line[col]);
                    if ( colDef.getCode()!=null )
                        row.get(colDef.getCode());
                }
                if ( colDef.getCustomPropertyName()!=null)
                    setProperty(colDef.getCustomPropertyName(), line[col]);

            } // ignoreMe
            col++;
        }
        return row;
    }

    @Override
    public boolean hasHeader() {
        return true;
    }

    @Override
    public char getDelimiter() {
        return ',';
    }
}
