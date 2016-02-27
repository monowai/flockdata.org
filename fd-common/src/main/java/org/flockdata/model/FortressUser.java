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
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.*;

@NodeEntity
@TypeAlias("FortressUser")
public class FortressUser {
    @GraphId
    Long id;

    //@Relationship( type = "BELONGS_TO", direction = Relationship.OUTGOING)
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
        return "FortressUser{" +
                "id=" + id +
                ", name='" + code + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    @JsonIgnore
    public String getKey() {
        return key;
    }
}
