/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

package org.flockdata.track.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.data.Company;
import org.flockdata.data.Fortress;
import org.flockdata.data.Segment;
import org.flockdata.registration.FortressResultBean;

/**
 * @author mike
 * @tag
 * @since 3/01/17
 */
public class SegmentResultBean implements Segment {
    private boolean isDefault;
    private String code;
    private FortressResultBean fortressResultBean;

    SegmentResultBean(){};

    public SegmentResultBean(Segment segment) {
        this();
        this.code = segment.getCode();
        this.fortressResultBean = new FortressResultBean(segment.getFortress());
        this.isDefault = segment.isDefault();
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    @JsonIgnore
    public Fortress getFortress() {
        return fortressResultBean;
    }

    @Override
    @JsonIgnore
    public Long getId() {
        return null;
    }

    @Override
    @JsonIgnore
    public String getKey() {
        return null;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    @Override
    @JsonIgnore
    public Company getCompany() {
        return null;
    }
}
