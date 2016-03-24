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

package org.flockdata.track;

import org.flockdata.model.Tag;

/**
 * Used in the location and caching of a Tag.
 * The properties that can are used to uniquely identify a tag
 *
 * Created by mike on 25/03/16.
 */
public class TagKey {
    String label;
    String prefix;
    String code;
    Tag tag ; // Carried in to help with cache eviction

    public TagKey(String label, String prefix, String code) {
        this.label = label;
        this.prefix = prefix;
        this.code = code;
    }

    public TagKey(Tag tag) {
        this.label = tag.getLabel();
        this.code = tag.getCode();
        this.tag = tag;
        // Figure out the prefix
        //this.prefix = tag.ge
    }

    public Tag getTag() {
        return tag;
    }

    public String getLabel() {
        return label;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getCode() {
        return code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TagKey)) return false;

        TagKey tagKey = (TagKey) o;

        if (label != null ? !label.equals(tagKey.label) : tagKey.label != null) return false;
        if (prefix != null ? !prefix.equals(tagKey.prefix) : tagKey.prefix != null) return false;
        return code != null ? code.equals(tagKey.code) : tagKey.code == null;

    }

    @Override
    public int hashCode() {
        int result = label != null ? label.hashCode() : 0;
        result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
        result = 31 * result + (code != null ? code.hashCode() : 0);
        return result;
    }
}
