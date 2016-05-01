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

import org.flockdata.helper.TagHelper;
import org.neo4j.graphdb.Node;

import java.util.HashMap;
import java.util.Map;

/**
 * User: mike
 * Date: 27/11/14
 * Time: 2:41 PM
 */
public class FdNode {
    Map<String,Object>data = new HashMap<>();
    public FdNode(Node node){
        data.put("id", node.getId());
        if ( node.hasProperty("name") ) {
            data.put("name", node.getProperty("name"));
            data.put("code", node.getProperty("code"));
        } else {
            data.put("name", node.getProperty("code"));
        }
        data.put("nodeType", TagHelper.getLabel(node.getLabels()));
        data.put("tag", true);

    }

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

    public String getNodeType(){
        return data.get("nodeType").toString();
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
