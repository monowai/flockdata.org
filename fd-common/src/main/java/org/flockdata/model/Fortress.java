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

package org.flockdata.model;

import org.flockdata.registration.bean.FortressInputBean;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.search.model.EntitySearchSchema;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.*;

import java.io.Serializable;
import java.util.Locale;
import java.util.TimeZone;

@NodeEntity
@TypeAlias("Fortress")
public class Fortress implements Serializable {

    @GraphId
    Long id;

    //@Indexed
    private String code;

    //@Indexed
    private String name;

    @RelatedTo(type = "OWNS", direction = Direction.INCOMING)
    @Fetch
    private Company company;

    private Boolean accumulatingChanges = false;
    private Boolean searchActive = true;
    private String timeZone;
    private String languageTag;
    private Boolean system = Boolean.FALSE;
    private Boolean enabled = Boolean.TRUE;
    private Boolean storeEnabled = Boolean.TRUE;

    @Indexed (unique = true)
    private String indexName = null;

    protected Fortress() {
    }

    public Fortress(FortressInputBean fortressInputBean, org.flockdata.model.Company ownedBy) {
        this();
        getTimeZone();
        getLanguageTag();
        setFortressInput(fortressInputBean);
        setCompany(ownedBy);

    }

    public String getIndexName() {
        if ( indexName == null )
            indexName = EntitySearchSchema.parseIndex(this);
        return indexName;
    }

    Fortress setFortressInput(FortressInputBean fortressInputBean) {
        setName(fortressInputBean.getName().trim());
        setSearchActive(fortressInputBean.getSearchActive());
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
        this.code = name.toLowerCase().replaceAll("\\s+", "");
    }

    @JsonIgnore
    public Company getCompany() {
        return company;
    }

    public void setCompany(org.flockdata.model.Company ownedBy) {
        this.indexName = EntitySearchSchema.parseIndex(ownedBy.getCode(),getCode() );
        this.company = ownedBy;

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

    public Boolean isSearchActive() {
        return searchActive;
    }

    public void setSearchActive(Boolean searchActive) {
        if (searchActive != null)
            this.searchActive = searchActive;
    }

    public void setAccumulatingChanges(Boolean addChanges) {
        this.accumulatingChanges = addChanges;
        if (addChanges)
            searchActive = false;
    }

    @Override
    public String toString() {
        String companyName ;
        String companyId;
        if ( company!=null ) {
            companyName = company.getName();
            companyId = company.getId().toString();
        } else {
            companyId ="[NULL]";
            companyName ="[NULL]";
        }
        return "FortressNode{" +
                "id=" + id +
                ", name='" + name + '\'' +
                (company !=null ? ", company='" + companyName + '\'' : ", companyId='" + companyId + '\'') +
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Fortress)) return false;

        Fortress that = (Fortress) o;

        if (code != null ? !code.equals(that.code) : that.code != null) return false;
        if (company != null ? !company.equals(that.company) : that.company != null) return false;
        if (indexName != null ? !indexName.equals(that.indexName) : that.indexName != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = indexName != null ? indexName.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (company != null ? company.hashCode() : 0);
        return result;
    }
}
