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

package org.flockdata.query;

/**
 * Pojo for carrying matrix result data
 * User: mike
 * Date: 19/04/14
 * Time: 6:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class EdgeResult {
    private String source;
    private String target;
    private Number count;

    public EdgeResult(String source, String target, Number count) {
        this();
        this.source = source;
        this.target = target;
        this.count = count;

    }

    public EdgeResult() {
    }

    public String getSource() {
        return source;
    }

    /**
     *
     * @deprecated  use getSource()
     * @return source
     */
//    public String getFrom() {return getSource(); }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

//    public String getTo() {return getTarget();}

    public void setTarget(String target) {
        this.target = target;
    }

    public Number getCount() {
        return count;
    }

    public void setCount(Number count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "EdgeResult{" +
                "source='" + source + '\'' +
                ", target='" + target + '\'' +
                ", count=" + count +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EdgeResult)) return false;

        EdgeResult that = (EdgeResult) o;

        if (source != null ? !source.equals(that.source) : that.source != null) return false;
        return !(target != null ? !target.equals(that.target) : that.target != null);

    }

    @Override
    public int hashCode() {
        int result = source != null ? source.hashCode() : 0;
        result = 31 * result + (target != null ? target.hashCode() : 0);
        return result;
    }

}
