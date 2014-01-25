package com.auditbucket.client;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * User: Mike Holdsworth
 * Since: 20/11/13
 */
public interface Mappable {

    @JsonIgnore
    Importer.importer getImporter();

    @JsonIgnore
    AbRestClient.type getABType();
}
