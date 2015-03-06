/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

package org.flockdata.test.engine;

import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Tag;

import java.util.Map;

/**
 * Created by mike on 6/03/15.
 */
public class SimpleTag implements Tag {

    TagInputBean tagInputBean;
    private String code;

    public SimpleTag () {}

    public SimpleTag(TagInputBean tagInputBean){
        this();
        this.tagInputBean = tagInputBean;
        this.code = tagInputBean.getCode();
    }
    @Override
    public String getName() {
        return tagInputBean.getName();
    }

    @Override
    public void setName(String floppy) {

    }

    @Override
    public Long getId() {
        return 1l;
    }

    @Override
    public String getKey() {
        return code;
    }

    @Override
    public Object getProperty(String num) {
        return tagInputBean.getProperties().get(num);
    }

    @Override
    public Map<String, Object> getProperties() {
        return tagInputBean.getProperties();
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getLabel() {
        return tagInputBean.getLabel();
    }

    public void setCode(String code) {
        this.code = code;
    }
}
