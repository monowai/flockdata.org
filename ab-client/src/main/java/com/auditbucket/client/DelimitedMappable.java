package com.auditbucket.client;

/**
 * User: Mike Holdsworth
 * Since: 25/01/14
 */

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Support class to handle mapping from one format to another format
 * User: Mike Holdsworth
 * Since: 13/10/13
 */
public interface DelimitedMappable extends Mappable {

    String setData(String[] headerRow, String[] line) throws JsonProcessingException;

    @JsonIgnore
    boolean hasHeader();

    DelimitedMappable newInstance();

    @JsonIgnore
    char getDelimiter();


}
