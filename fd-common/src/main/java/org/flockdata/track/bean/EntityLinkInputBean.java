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

import org.flockdata.registration.FortressInputBean;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Used to handle cross references from a source Entity through to a collection of named references
 * User: mike
 * Date: 2/04/14
 * Time: 12:24 PM
 */
public class EntityLinkInputBean {
    Map<String,List<EntityKeyBean>>references;
    private String fortress;
    private String documentType;
    private String code; // Callers key
    private String serviceMessage;
    private Map<String,Collection<EntityKeyBean>>ignored;

    protected EntityLinkInputBean(){}

    public EntityLinkInputBean(EntityInputBean entityInputBean) {
        this(entityInputBean.getFortress(), entityInputBean.getDocumentType(), entityInputBean.getCode());
        this.references = entityInputBean.getEntityLinks();
    }

    /**
     *  @param fortress          Parent fortress
     * @param documentName      Parent docType
     * @param code              Parent code reference
     */
    public EntityLinkInputBean(FortressInputBean fortress, DocumentTypeInputBean documentName, String code) {
        this.code = code;
        this.fortress = fortress.getName();
        this.documentType = documentName.getName();
    }

    public String getCode() {
        return code;
    }

    public String getFortress() {
        return fortress;
    }

    public Map<String,List<EntityKeyBean>>getReferences(){
        return references;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityLinkInputBean)) return false;

        EntityLinkInputBean that = (EntityLinkInputBean) o;

        if (code != null ? !code.equals(that.code) : that.code != null) return false;
        if (documentType != null ? !documentType.equals(that.documentType) : that.documentType != null) return false;
        return !(fortress != null ? !fortress.equals(that.fortress) : that.fortress != null);

    }

    @Override
    public int hashCode() {
        int result = fortress != null ? fortress.hashCode() : 0;
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (documentType != null ? documentType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CrossReferenceInputBean{" +
                "code='" + code + '\'' +
                ", references=" + references.size() +
                ", fortress='" + fortress + '\'' +
                ", docType ='" + documentType + '\'' +
                '}';
    }

    public void setServiceMessage(String serviceMessage) {
        this.serviceMessage = serviceMessage;
    }

    public String getServiceMessage() {
        return serviceMessage;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setIgnored(String xRefName, Collection<EntityKeyBean> ignored) {
        if (this.ignored == null )
           this.ignored = new HashMap<>();
        this.ignored.put(xRefName, ignored);
    }

    public Map<String, Collection<EntityKeyBean>> getIgnored() {
        return ignored;
    }
}
