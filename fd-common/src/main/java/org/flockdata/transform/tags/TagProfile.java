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

package org.flockdata.transform.tags;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.GeoDefinition;
import org.flockdata.transform.GeoDeserializer;
import org.flockdata.transform.GeoPayload;

import java.util.ArrayList;

/**
 * User: mike
 * Date: 27/05/14
 * Time: 3:51 PM
 */
public class TagProfile implements GeoDefinition {
    private String name;
    private String code;
    private String keyPrefix;

    private Boolean reverse =false;
    private String notFound;

    private String relationship;
    private String delimiter =null;
    private boolean country = false;
    private String label;

    private String condition;// boolean expression that determines if this tag will be created
    private ArrayList<TagProfile> targets;
    private ArrayList<ColumnDefinition>properties;
    private ArrayList<ColumnDefinition>rlxProperties;
    private ArrayList<AliasInputBean>aliases;

    @JsonDeserialize(using = GeoDeserializer.class)
    private GeoPayload geoData;

    private boolean mustExist;
    private boolean merge;

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

    public String getKeyPrefix() {
        return keyPrefix;
    }

    /**
     *
     * @return The label node that will be created. Reverts to the column name if not defined
     */
    public String getLabel() {
        if ( label == null)
            return code;
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
                "code='" + code + '\'' +
                ", relationship='" + relationship + '\'' +
                ", label='" + label + '\'' +
                '}';
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

    public ArrayList<ColumnDefinition> getRlxProperties() {
        return rlxProperties;
    }

    public boolean isMustExist() {
        return mustExist;
    }

    public boolean hasProperites() {
        return properties!=null && properties.size()>0;
    }

    public void setAliases(ArrayList<AliasInputBean> aliases) {
        this.aliases = aliases;
    }

    public ArrayList<AliasInputBean> getAliases() {
        return aliases;
    }

    public boolean hasAliases (){
        return aliases!=null && aliases.size()>0;
    }

    public String getNotFound() {
        return notFound;
    }

    @Override
    public GeoPayload getGeoData() {
        return geoData;
    }

    public boolean isMerge() {
        return merge;
    }

    public void setMerge(boolean merge) {
        this.merge = merge;
    }
}
