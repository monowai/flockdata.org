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

package org.flockdata.search.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.*;

/**
 * POJO representing a TagCloud .
 * Example : {"colera":12,"Cancer":1};
 * User: Mike
 * Date: 12/10/2014
 */
public class TagCloud {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Long> terms;

    @JsonIgnore
    private Map<String, Long> workingMap;

    private int maxEntries;

    public TagCloud() {
        this(50);
    }

    public TagCloud(int maxEntries) {
        this.maxEntries = maxEntries;
        terms = new HashMap<>();
        workingMap = new HashMap<>();
    }

    public Map<String, Long> getTerms() {
        return terms;
    }

    public void setTerms(Map<String, Long> terms) {
        this.terms = terms;
    }

    private Long minValue = 0l;
    private Long maxValue = 0l;
    private String smallestTerm = null;

    public void addTerm(String term, long occurrence) {
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
        for (String key : workingMap.keySet()) {
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
