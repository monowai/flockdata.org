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

package org.flockdata.profile;

import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * All the validation results. Maps are indexed from 0
 *
 * Created by mike on 14/04/16.
 */
public class ContentValidationResults {

    private Map<Integer,EntityInputBean> entity = new HashMap<>();
    private Map<Integer,Collection<TagInputBean>> tags= new HashMap<>();

    private Map<Integer,Collection<ColumnValidationResult>> results = new HashMap<>();
    private Map<Integer,String> message= new HashMap<>();

    public Collection<ColumnValidationResult> getResults(Integer row) {
        return results.get(row);
    }

    // 0 based row index
    public EntityInputBean getEntity(Integer row) {
        return entity.get(row);
    }

    public ContentValidationResults add(Integer row, Collection<TagInputBean> tags) {
        this.tags .put(row, tags);
        return this;
    }

    public ContentValidationResults add(Integer row, EntityInputBean entityInputBean) {
        this.entity.put(row,entityInputBean);
        return this;
    }


    public void setMessage(int row, String message) {
        this.message.put(row, message);
    }

    public String getMessage(int row ) {
        return message.get(row);
    }

    public void addResults(int row, Collection<ColumnValidationResult> validatedResults) {
        this.results.put(row, validatedResults);
    }

    public Map<Integer, EntityInputBean> getEntity() {
        return entity;
    }

    public Map<Integer, Collection<TagInputBean>> getTags() {
        return tags;
    }

    public Map<Integer, Collection<ColumnValidationResult>> getResults() {
        return results;
    }

    public Map<Integer, String> getMessage() {
        return message;
    }
}
