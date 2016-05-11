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

package org.flockdata.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.registration.FortressInputBean;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.*;

import java.io.Serializable;
import java.util.Locale;
import java.util.TimeZone;

@NodeEntity
@TypeAlias("Fortress")
public class Fortress implements MetaFortress, Serializable {

    @GraphId
    Long id;

    @Indexed
    private String code;

    @Indexed
    private String name;

    //@Relationship(type = "OWNS", direction = Relationship.INCOMING)
    @RelatedTo(type = "OWNS", direction = Direction.INCOMING)
    @Fetch
    private Company company;


    @RelatedTo(type = "DEFAULT", direction = Direction.OUTGOING)
    @Fetch
    private FortressSegment defaultSegment;

    private Boolean accumulatingChanges = false;
    private Boolean storeEnabled = Boolean.TRUE;
    private Boolean searchEnabled = true;
    private String timeZone;
    private String languageTag;
    private Boolean system = Boolean.FALSE;
    private Boolean enabled = Boolean.TRUE;

    @Indexed (unique = true)
    private String rootIndex = null;

    protected Fortress() {
    }

    public Fortress(FortressInputBean fortressInputBean, org.flockdata.model.Company ownedBy) {
        this();
        getTimeZone();
        getLanguageTag();
        setFortressInput(fortressInputBean);
        setCompany(ownedBy);
        defaultSegment = new FortressSegment(this);

    }

    public String getRootIndex() {
        return rootIndex;
    }

    Fortress setFortressInput(FortressInputBean fortressInputBean) {
        setName(fortressInputBean.getName().trim());
        setSearchEnabled(fortressInputBean.getSearchActive());
        system = fortressInputBean.getSystem();
        enabled = fortressInputBean.getEnabled();
        storeEnabled = fortressInputBean.getStoreActive();
        if (fortressInputBean.getTimeZone() != null) {
            this.timeZone = fortressInputBean.getTimeZone();
            if (TimeZone.getTimeZone(timeZone) == null)
                throw new IllegalArgumentException(fortressInputBean.getTimeZone() + " is not a valid TimeZone. If you don't know a timezone to set, leave this null and the system default will be used.");
        }
        if (fortressInputBean.getLanguageTag() != null)
            this.languageTag = fortressInputBean.getLanguageTag();
        else
            getLanguageTag();


        return this;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.code = code(name);
    }

    public static String code(String name){
        if ( name == null )
            return null;
        return name.toLowerCase().replaceAll("\\s+", "");
    }
    @JsonIgnore
    public Company getCompany() {
        return company;
    }

    public void setCompany(org.flockdata.model.Company company) {
        this.company = company;

    }

    public Boolean isStoreEnabled() {
        if ( storeEnabled == null )
            return Boolean.TRUE;
        return storeEnabled;
    }

    @JsonIgnore
    public Boolean isStoreDisabled() {
        return !isStoreEnabled();
    }

    public void setStoreEnabled(Boolean enabled) {
        this.storeEnabled = enabled;
    }

    @JsonIgnore
    public Boolean isAccumulatingChanges() {
        // Reserved for future use
        return accumulatingChanges;
    }

    public Boolean isSearchEnabled() {
        return searchEnabled;
    }

    public Fortress setSearchEnabled(Boolean searchEnabled) {
        if (searchEnabled != null)
            this.searchEnabled = searchEnabled;
        return this;
    }

    public void setAccumulatingChanges(Boolean addChanges) {
        this.accumulatingChanges = addChanges;
        if (addChanges)
            searchEnabled = false;
    }

    @Override
    public String toString() {
        return "Fortress{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", searchEnabled=" + searchEnabled +
                ", storeEnabled=" + storeEnabled +
                ", rootIndex='" + rootIndex + '\'' +
                '}';
    }

    public String getTimeZone() {
        if (this.timeZone == null)
            this.timeZone = TimeZone.getDefault().getID();
        return timeZone;
    }

    public String getLanguageTag() {
        if (this.languageTag == null)
            this.languageTag = Locale.getDefault().toLanguageTag();
        return this.languageTag;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getCode() {
        return code;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public Boolean isSystem() {
        return system;
    }

    @JsonIgnore
    public FortressSegment getDefaultSegment() {
        return defaultSegment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Fortress)) return false;

        Fortress that = (Fortress) o;

        if (code != null ? !code.equals(that.code) : that.code != null) return false;
        if (company != null ? !company.equals(that.company) : that.company != null) return false;
        if (rootIndex != null ? !rootIndex.equals(that.rootIndex) : that.rootIndex != null) return false;
        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        int result = rootIndex != null ? rootIndex.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (company != null ? company.hashCode() : 0);
        return result;
    }

    public void setRootIndex(String rootIndex) {
        this.rootIndex = rootIndex;
    }

    public void setSystem(Boolean system) {
        this.system = system;
    }
}
