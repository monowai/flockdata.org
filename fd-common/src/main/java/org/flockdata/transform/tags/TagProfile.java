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

package org.flockdata.transform.tags;

import org.flockdata.transform.ColumnDefinition;

import java.util.ArrayList;

/**
 * User: mike
 * Date: 27/05/14
 * Time: 3:51 PM
 */
public class TagProfile {
    private String column;
    private String name;
    private String nameExp;
    private String code;

    private String codeExp;

    private Boolean reverse =false;
    private String relationship;
    private String delimiter =null;
    private boolean country = false;
    private String label;

    private String condition;// boolean expression that determines if this tag will be created
    private ArrayList<TagProfile> targets;
    private ArrayList<ColumnDefinition>properties;

    private boolean mustExist;

    public String getColumn() {
        return column;
    }

    public void setColumn(String name) {
        this.column = name;
    }

    public Boolean getReverse() {
        return reverse;
    }

    public void setReverse(Boolean reverse) {
        this.reverse = reverse;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getName() {
        return name;
    }

    /**
     *
     * @return The label node that will be created. Reverts to the column name if not defined
     */
    public String getLabel() {
        if ( label == null)
            return column;
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public ArrayList<TagProfile> getTargets() {
        return targets;
    }

    public void setTargets(ArrayList<TagProfile>  targets) {
        this.targets = targets;
    }

    @Override
    public String toString() {
        return "CsvTag{" +
                "column='" + column + '\'' +
                ", relationship='" + relationship + '\'' +
                ", label='" + label + '\'' +
                '}';
    }

    public boolean getMustExist() {
        return mustExist;
    }

    public void setMustExist(boolean mustExist) {
        this.mustExist = mustExist;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter){
        this.delimiter= delimiter;
    }

    public boolean isCountry() {
        return country;
    }

    public void setCountry(boolean country) {
        this.country = country;
    }

    public String getCondition() {
        return condition;
    }

    public ArrayList<ColumnDefinition> getProperties() {
        return properties;
    }

    public String getNameExp() {
        return nameExp;
    }

    public String getCodeExp() {
        return codeExp;
    }

    public boolean isMustExist() {
        return mustExist;
    }

    public boolean hasProperites() {
        return properties!=null && properties.size()>0;
    }
}
