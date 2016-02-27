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

package org.flockdata.registration;

/**
 * Payload representing alias attributes for a tag.
 * The AliasInput can be set in to a ColumnDefinitionInputBean or TagInputBean
 *
 * Created by mike on 12/02/15.
 */
public class AliasInputBean {
    private String code;
    private String description;

    public AliasInputBean(){}

    public AliasInputBean(String code) {
        this();
        this.code = code;
    }

    public AliasInputBean(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public AliasInputBean setCode(String code) {
        this.code = code;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public AliasInputBean setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AliasInputBean)) return false;

        AliasInputBean that = (AliasInputBean) o;

        return code.equals(that.code);

    }

    @Override
    public int hashCode() {
        int result = code.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "AliasInputBean{" +
                "code='" + code + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
