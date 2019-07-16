/*
 *  Copyright 2012-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.test.unit;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.ExpressionHelper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

/**
 * @author mholdsworth
 * @since 30/07/2016
 */
public class TestDateConversion {

    @Test
    public void longFormDateTime() throws Exception {
        String convert = "Fri Sep 10 18:14:22 +0100 2010";

        ColumnDefinition columnDefinition = new ColumnDefinition();
        columnDefinition.setDataType("date");
        columnDefinition.setDateFormat("Automatic");

        Long converted = ExpressionHelper.parseDate(columnDefinition, convert);
        assertTrue("Didn't resolve", converted != 0L);


        DateTime resolved = new DateTime(converted, DateTimeZone.forID("UTC"));
        assertEquals(2010, resolved.getYear());
        assertEquals(9, resolved.getMonthOfYear());
        assertEquals(10, resolved.getDayOfMonth());
        assertEquals(17, resolved.getHourOfDay());
        assertEquals(14, resolved.getMinuteOfHour());

    }

    @Test
    public void customFormat() throws Exception {
        String convert = "2015-12-01";

        ColumnDefinition columnDefinition = new ColumnDefinition();
        columnDefinition.setDataType("date");
        columnDefinition.setDateFormat("yyyy-MM-dd");

        Long converted = ExpressionHelper.parseDate(columnDefinition, convert);
        assertTrue("Didn't resolve", converted != 0L);

        DateTime resolved = new DateTime(converted);
        assertEquals(2015, resolved.getYear());
        assertEquals(12, resolved.getMonthOfYear());
        assertEquals(1, resolved.getDayOfMonth());
    }

    @Test
    public void customFormatDmy() throws Exception {
        String convert = "01/10/2015";

        ColumnDefinition columnDefinition = new ColumnDefinition();
        columnDefinition.setDataType("date");
        columnDefinition.setDateFormat("dd/MM/yyyy");

        Long converted = ExpressionHelper.parseDate(columnDefinition, convert);
        assertTrue("Didn't resolve", converted != 0L);

        DateTime resolved = new DateTime(converted);
        assertEquals(2015, resolved.getYear());
        assertEquals(10, resolved.getMonthOfYear());
        assertEquals(1, resolved.getDayOfMonth());
    }

    @Test
    public void convertFromEpoc() throws Exception {
        String epoc = Long.toString(System.currentTimeMillis() / 1000);  // Linux epoc

        DateTime now = new DateTime();

        ColumnDefinition columnDefinition = new ColumnDefinition();
        columnDefinition.setDataType("date");
        columnDefinition.setDateFormat("epoc");

        Long converted = ExpressionHelper.parseDate(columnDefinition, epoc);
        assertTrue("Didn't resolve", converted != 0L);
        DateTime resolved = new DateTime(converted);
        assertEquals(now.getYear(), resolved.getYear());
        assertEquals(now.getMonthOfYear(), resolved.getMonthOfYear());
        assertEquals(now.getDayOfMonth(), resolved.getDayOfMonth());

    }
}
