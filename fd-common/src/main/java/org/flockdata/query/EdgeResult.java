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
