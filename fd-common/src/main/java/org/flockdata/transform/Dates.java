/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

package org.flockdata.transform;

import org.flockdata.data.ContentModel;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;

import java.util.Map;

/**
 * Helper class for Spring Expression to extract portions of a date
 * @author mholdsworth
 * @since 27/06/2016
 */
public class Dates {

    /**
     * Extract portions of a date. in this example, the year is extracted from the data as the column asAtDate.
     * properties required to do the date transformation are stored in the #model[dateColumn]
     * <p>
     * given 2015-04-28
     * year  2015
     * month 02 <- always 2 digits
     * dom   28 <- always 2 digits
     * dow
     * "segment": "T(org.flockdata.transform.Dates).get('asAtDate', 'year', #model, #data)"
     *
     * Default delimiter is '-' as it's web-safe.
     *
     * @param dateColumn   the name of the source column
     * @param portion      year-month-dow-dom-hour dash delimited format. each element is extracted and returned
     * @param contentModel Contains all column definitions
     * @param data         The datarow. Makes the syntax look neater
     * @return {portion}-{portion}.....
     */
    public static Object get(String dateColumn, String portion, ContentModel contentModel, Map<String, Object> data) {
        return get(dateColumn, "-", portion, contentModel, data);
    }

    /**
     * As above, but using a custom delimiter
     */
    public static Object get(String dateColumn, String delimiter, String portion, ContentModel contentModel, Map<String, Object> data) {

        Long dateResult = ExpressionHelper.parseDate(contentModel.getColumnDef(dateColumn), data.get(dateColumn).toString());
        DateTime dateTime = new DateTime(dateResult);

        String[] fields = portion.split(delimiter);
        String result = null;

        for (String field : fields) {
            if (field.equalsIgnoreCase("year"))
                result = addToResult(result, delimiter, Integer.toString(dateTime.get(DateTimeFieldType.year())));
            else if (field.equalsIgnoreCase("month"))
                result = addToResult(result, delimiter, String.format("%02d", dateTime.get(DateTimeFieldType.monthOfYear())));
            else if (field.equalsIgnoreCase("dom"))
                result = addToResult(result, delimiter, String.format("%02d", dateTime.get(DateTimeFieldType.dayOfMonth())));
            else if (field.equalsIgnoreCase("dow"))
                result = addToResult(result, delimiter, String.format("%02d", dateTime.get(DateTimeFieldType.dayOfWeek())));
            else if (field.equalsIgnoreCase("hour"))
                result = addToResult(result, delimiter, String.format("%02d", dateTime.get(DateTimeFieldType.hourOfDay())));
        }

        return result;
    }

    private static String addToResult(String result, String delimiter, String value) {
        if (result != null && value != null)
            result= result + delimiter + value;
        else
            result = value;
        return result;
    }
}
