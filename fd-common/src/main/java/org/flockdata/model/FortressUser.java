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

import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.*;

@NodeEntity
@TypeAlias("FortressUser")
public class FortressUser {
    @GraphId
    Long id;

    @RelatedTo( type = "BELONGS_TO", direction = Direction.OUTGOING)
    @Fetch
    private Fortress fortress;

    @Indexed(unique = true)
    private String key = null ;

    @Indexed
    private String code = null;

    private String name;

    protected FortressUser() {
    }

    public FortressUser(Fortress fortress, String fortressUserName) {
        this();
        setCode(fortressUserName);
        key =fortress.getId()+"."+getCode();
        setFortress(fortress);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        if (code != null ) {
            this.code = code.toLowerCase();
            this.name = code;
        }
    }

    public Fortress getFortress() {
        return fortress;
    }

    public void setFortress(Fortress fortress) {
        this.fortress = fortress;
    }

    @Override
    public String toString() {
        return "FortressUserNode{" +
                "id=" + id +
                ", name='" + code + '\'' +
                '}';
    }


    public String getName() {
        return name;
    }
}
