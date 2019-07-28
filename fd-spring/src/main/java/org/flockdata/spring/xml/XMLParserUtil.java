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
    if (!element.hasAttribute(attribute)) {
      return defaultValue;
    }
    return Boolean
        .valueOf(getElementStringValue(element, attribute));
  }

  public static String getElementStringValue(Element element, String attribute) {
    return element.getAttribute(attribute);
  }
}
