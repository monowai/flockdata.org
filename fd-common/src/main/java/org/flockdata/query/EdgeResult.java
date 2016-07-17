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
 * Pojo for carrying matrix result data
 * User: mike
 * Date: 19/04/14
 * Time: 6:52 AM
 */
public class EdgeResult {
    private Map<String,Object> data = new HashMap<>();

    public EdgeResult(String source, String target, Number count) {
        this();
        data.put("source", source);
        data.put("target", target);
        data.put("count", count);

    }

    public EdgeResult() {
    }

    public Map<String,Object> getData(){
        return data;
    }

    public String getSource() {
        return data.get("source").toString();
    }

    public void setSource(String source) {
        data.put("source",source);
    }

    public String getTarget() {
        return getData().get("target").toString();
    }

    public void setTarget(String target) {
        getData().put("target", target);
    }

    public Number getCount() {

        return (Number)getData().get("count");
    }

    public void setCount(Number count) {
        getData().put("count", count);
    }

    @Override
    public String toString() {
        return "EdgeResult{" +
                "source='" + getSource() + '\'' +
                ", target='" + getTarget() + '\'' +
                ", count=" + getCount() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EdgeResult)) return false;

        EdgeResult that = (EdgeResult) o;

        if (getSource() != null ? !getSource().equals(that.getSource()) : that.getSource()!= null) return false;
        return !(getTarget() != null ? !getTarget().equals(that.getTarget()) : that.getTarget()!= null);

    }

    @Override
    public int hashCode() {
        int result = getSource() != null ? getSource().hashCode() : 0;
        result = 31 * result + (getTarget() != null ? getTarget().hashCode() : 0);
        return result;
    }

}
