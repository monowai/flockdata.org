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

package org.flockdata.spring.xml;

import org.w3c.dom.Element;

/**
 * XML Parser helpers
 * Source from: http://www.java2s.com/Tutorial/Java/0440__XML/GetElementBooleanValue.htm
 */
class XMLParserUtil {
    public static boolean getElementBooleanValue(Element element, String attribute) {
        return getElementBooleanValue(element, attribute, false);
    }

    private static boolean getElementBooleanValue(Element element, String attribute, boolean defaultValue) {
        if (!element.hasAttribute(attribute)) return defaultValue;
        return Boolean
                .valueOf(getElementStringValue(element, attribute))
                .booleanValue();
    }

    public static String getElementStringValue(Element element, String attribute) {
        return element.getAttribute(attribute);
    }
}
