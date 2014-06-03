package com.auditbucket.client.common;

import com.auditbucket.client.Importer;
import com.auditbucket.client.rest.AbRestClient;
import com.auditbucket.registration.bean.TagInputBean;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * User: mike
 * Date: 27/04/14
 * Time: 4:36 PM
 */
public class TagMapper extends TagInputBean implements DelimitedMappable {
    private boolean hasHeader = true;
    public TagMapper(ImportParams importParams) {
        setIndex(importParams.getDocumentType());
        hasHeader = importParams.hasHeader();
    }

    @Override
    public Importer.importer getImporter() {
        return Importer.importer.CSV;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public AbRestClient.type getABType() {
        return AbRestClient.type.TAG;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String setData(String[] headerRow, String[] line, ImportParams staticDataResolver) throws JsonProcessingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasHeader() {
        return true;
    }

    public static DelimitedMappable newInstance(ImportParams importParams) {
        return new TagMapper(importParams);
    }

    @Override
    public char getDelimiter() {
        return ',';  //To change body of implemented methods use File | Settings | File Templates.
    }
}
