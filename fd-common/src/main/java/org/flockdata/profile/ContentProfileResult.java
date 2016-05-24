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

package org.flockdata.profile;

import org.flockdata.model.Profile;

/**
 * Created by mike on 14/04/16.
 */
public class ContentProfileResult {

    private String key;
    private String name;
    private String documentType;
    private String fortress ;

    ContentProfileResult () {

    }

    public ContentProfileResult(Profile profile){
        this();
        this.key = profile.getKey();
        this.name = profile.getName();
        this.fortress = profile.getFortress().getName();
        this.documentType = profile.getDocument().getName();

    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getFortress() {
        return fortress;
    }
}
