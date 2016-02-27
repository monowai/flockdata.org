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

package org.flockdata.model;

import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 4:56 PM
 */
@NodeEntity(useShortNames = true)
@TypeAlias("Profile")
public class Profile {
    @GraphId
    private Long id;

    @Indexed(unique = true)
    private String profileKey;

    //@Relationship(type = "FORTRESS_PROFILE")
    @RelatedTo(type = "FORTRESS_PROFILE")
    private Fortress fortress;

    //@Relationship( type = "DOCUMENT_PROFILE")
    @RelatedTo( type = "DOCUMENT_PROFILE")
    private DocumentType document;

    private String content;

    Profile() {}

    public Profile(Fortress fortress, DocumentType documentType) {
        this();
        this.fortress = fortress;
        this.document = documentType;
        this.profileKey = parseKey(fortress, documentType);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getProfileKey() {
        return profileKey;
    }

    public static String parseKey(Fortress fortress, DocumentType documentType) {
        return fortress.getId() +"-"+documentType.getId();
    }

    public Long getId() {
        return id;
    }

}
