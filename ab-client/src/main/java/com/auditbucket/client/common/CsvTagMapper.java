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
import com.auditbucket.client.rest.AbRestClient;
import com.auditbucket.helper.FlockException;
import com.auditbucket.registration.bean.TagInputBean;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * User: mike
 * Date: 27/04/14
 * Time: 4:34 PM
 */
public class CsvTagMapper extends TagInputBean implements DelimitedMappable {
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(CsvTagMapper.class);
    private ImportParams importParams;

    public CsvTagMapper(ImportParams importParams) {
        this.importParams = importParams;
    }

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
            CsvColumnHelper columnHelper = new CsvColumnHelper(column, line[col], importParams.getColumnDef(headerRow[col]));
            if (!columnHelper.ignoreMe()) {

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
                        setMustExist(columnHelper.isMustExist())
                                .setLabel(columnHelper.isCountry() ? "Country" : index);

                        CsvHelper.setNestedTags(this, columnHelper.getColumnDefinition().getTargets(), row);
                    }
                }
                if (columnHelper.isTitle()) {
                    setName(line[col]);
                }
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
