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

package org.flockdata.query;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mike on 2/05/16.
 */
public class FdNode {
    Map<String,Object>data = new HashMap<>();
    public FdNode(){}

    public FdNode(String key, Object value) {
        data.put("id", key);
        data.put("name", value);
    }

    public Map<String,Object>getData(){
        return data;
    }

    //    @JsonIgnore
    public String getKey() {
        return data.get("id").toString();
    }

    //    @JsonIgnore
    public Object getName() {
        return data.get("name");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FdNode)) return false;

        FdNode fdNode = (FdNode) o;

        if (getKey() != null ? !getKey().equals(fdNode.getKey()) : fdNode.getKey() != null) return false;
        return !(getName()!= null ? !getName().equals(fdNode.getName()) : fdNode.getName() != null);

    }

    @Override
    public int hashCode() {
        int result = getKey() != null ? getKey().hashCode() : 0;
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FdNode{" +
                "key='" + getKey() + '\'' +
                ", value=" + getName() +
                '}';
    }
}