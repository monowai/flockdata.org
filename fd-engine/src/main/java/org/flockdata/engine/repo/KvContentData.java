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

package org.flockdata.engine.repo;

import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.model.KvContent;

import java.util.Map;

/**
 * User: mike
 * Date: 17/09/14
 * Time: 1:03 PM
 */
public class KvContentData implements KvContent {
    private String attachment;
    Map<String,Object> what;

    private KvContentData(){super();}

    public KvContentData(ContentInputBean content){
        this();
        this.attachment = content.getAttachment();
        this.what = content.getWhat();
    }

    public KvContentData(Map<String, Object> result) {
        this();
        this.what = result;
    }

    public String getAttachment() {
        return attachment;
    }

    public Map<String, Object> getWhat() {
        return what;
    }

}
