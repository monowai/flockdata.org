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

package org.flockdata.track.bean;

import java.util.Map;

/**
 */
public class DeltaResultBean {
    private Map<String, Object> added;
    private Map<String, Object> changed;
    private Map<String, Object> removed;
    private Map<String, Object> unchanged;

    public Map getAdded() {
        return added;
    }

    public Map getChanged() {
        return changed;
    }

    public Map getRemoved() {
        return removed;
    }

    public void setAdded(Map<String, Object> added) {
        this.added = added;
    }

    public void setRemoved(Map<String, Object> removed) {
        this.removed = removed;
    }

    public void setChanged(Map<String, Object> changed) {
        this.changed = changed;
    }


    public void setUnchanged(Map<String, Object> unchanged) {
        this.unchanged = unchanged;
    }

    public Map<String, Object> getUnchanged() {
        return unchanged;
    }
}
