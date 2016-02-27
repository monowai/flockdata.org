/*
 *  Copyright 2012-2016 the original author or authors.
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

package org.flockdata.search.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

/**
 * POJO representing a TagCloud .
 * Example : {"colera":12,"Cancer":1};
 * User: Mike
 * Date: 12/10/2014
 */
public class TagCloud {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<Object, Long> terms;

    @JsonIgnore
    private Map<Object, Long> workingMap;

    private int maxEntries;

    public TagCloud() {
        this(50);
    }

    public TagCloud(int maxEntries) {
        this.maxEntries = maxEntries;
        terms = new HashMap<>();
        workingMap = new HashMap<>();
    }

    public Map<Object, Long> getTerms() {
        return terms;
    }

    public void setTerms(Map<Object, Long> terms) {
        this.terms = terms;
    }

    private Long minValue = 0l;
    private Long maxValue = 0l;
    private Object smallestTerm = null;

    public void addTerm(Object term, long occurrence) {
        if (workingMap.isEmpty()) {
            minValue = occurrence;
            maxValue = occurrence;
            smallestTerm = term;
        }

        if (workingMap.size() >= maxEntries) {
            // We have to get rid of a random small value
            if ( occurrence > workingMap.get(smallestTerm)) {
                workingMap.remove(smallestTerm);
                smallestTerm = term;
            } else
                return;
        }

        // Value has been accepted, so track max and min for subsequent scaling operations.
        if ( occurrence > maxValue)
            maxValue = occurrence;
        if ( occurrence < minValue)
            minValue = occurrence;

        workingMap.put(term, occurrence);

    }

    public void scale() {
        for (Object key : workingMap.keySet()) {
            Long value = workingMap.get(key);
            Double scaled = scale(Double.valueOf(value.toString()), minValue.doubleValue(), maxValue.doubleValue(), 15, 60);
            terms.put(key, scaled.longValue());
        }

    }
    public static double scale(final double valueIn, final double baseMin, final double baseMax, final double limitMin, final double limitMax) {
        //https://stackoverflow.com/questions/5294955/how-to-scale-down-a-range-of-numbers-with-a-known-min-and-max-value
        return ((limitMax - limitMin) * (valueIn - baseMin) / (baseMax - baseMin)) + limitMin;
    }
}
