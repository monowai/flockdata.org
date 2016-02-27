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
