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

import org.flockdata.profile.model.ContentModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Payload required to validate Content with a selection of data
 *
 * Created by mike on 14/04/16.
 */
public class ContentValidationRequest {

    ContentModel contentModel;
    Collection<Map<String,Object>> rows;

    public ContentValidationRequest(){}

    public ContentValidationRequest(ContentModel model) {
        this();
        this.contentModel = model;
    }

    public ContentValidationRequest(Map<String, Object> dataMap) {
        Collection<Map<String,Object>>rows = new ArrayList<>();
        rows.add(dataMap);
        this.rows = rows;
    }

    public ContentModel getContentModel() {
        return contentModel;
    }

    public Collection<Map<String, Object>> getRows() {
        return rows;
    }
}
