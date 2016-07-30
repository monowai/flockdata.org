/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.helper;

import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.ExpressionHelper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * Created by mike on 30/07/16.
 */
public class TestDateConversion {

    @Test
    public void longFormDateTime() throws Exception{
        String convert = "Fri Sep 10 18:14:22 +0100 2010";

        ColumnDefinition columnDefinition = new ColumnDefinition();
        columnDefinition.setDataType("date");
        columnDefinition.setDateFormat("Automatic");

        Long converted = ExpressionHelper.parseDate(columnDefinition, convert);
        assertTrue("Didn't resolve", converted!=0L);


        DateTime resolved = new DateTime(converted, DateTimeZone.forID("UTC"));
        assertEquals(2010, resolved.getYear());
        assertEquals(9, resolved.getMonthOfYear());
        assertEquals(10, resolved.getDayOfMonth());
        assertEquals(17, resolved.getHourOfDay());
        assertEquals(14, resolved.getMinuteOfHour());

    }

    @Test
    public void customFormat() throws Exception{
        String convert = "2015-12-01";

        ColumnDefinition columnDefinition = new ColumnDefinition();
        columnDefinition.setDataType("date");
        columnDefinition.setDateFormat("yyyy-MM-dd");

        Long converted = ExpressionHelper.parseDate(columnDefinition, convert);
        assertTrue("Didn't resolve", converted!=0L);

        DateTime resolved = new DateTime(converted);
        assertEquals(2015, resolved.getYear());
        assertEquals(12, resolved.getMonthOfYear());
        assertEquals(1, resolved.getDayOfMonth());
    }

    @Test
    public void customFormatDmy() throws Exception{
        String convert = "01/10/2015";

        ColumnDefinition columnDefinition = new ColumnDefinition();
        columnDefinition.setDataType("date");
        columnDefinition.setDateFormat("dd/MM/yyyy");

        Long converted = ExpressionHelper.parseDate(columnDefinition, convert);
        assertTrue("Didn't resolve", converted!=0L);

        DateTime resolved = new DateTime(converted);
        assertEquals(2015, resolved.getYear());
        assertEquals(10, resolved.getMonthOfYear());
        assertEquals(1, resolved.getDayOfMonth());
    }

    @Test
    public void convertFromEpoc () throws Exception {
        String epoc = Long.toString(System.currentTimeMillis()/1000);  // Linux epoc

        DateTime now = new DateTime();

        ColumnDefinition columnDefinition = new ColumnDefinition();
        columnDefinition.setDataType("date");
        columnDefinition.setDateFormat("epoc");

        Long converted = ExpressionHelper.parseDate(columnDefinition, epoc);
        assertTrue("Didn't resolve", converted!=0L);
        DateTime resolved = new DateTime(converted);
        assertEquals(now.getYear(), resolved.getYear());
        assertEquals(now.getMonthOfYear(), resolved.getMonthOfYear());
        assertEquals(now.getDayOfMonth(), resolved.getDayOfMonth());

    }
}
