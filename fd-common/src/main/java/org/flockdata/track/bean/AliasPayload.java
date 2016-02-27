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

import org.flockdata.registration.AliasInputBean;

/**
 * Data necessary to create a single Alias for a tag
 *
 * Created by mike on 3/07/15.
 */
public class AliasPayload {
    String label;
    AliasInputBean aliasInput;
    Long tagId;

    AliasPayload(){}


    public AliasPayload(String label, Long tagId, AliasInputBean aliasInput) {
        this();
        this.label = label;
        this.aliasInput = aliasInput;
        this.tagId = tagId;

    }

    public String getLabel() {
        return label;
    }

    public AliasInputBean getAliasInput() {
        return aliasInput;
    }

    public Long getTagId() {
        return tagId;
    }
}
