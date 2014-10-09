package com.auditbucket.transform;

import com.auditbucket.profile.ImportProfile;
import com.auditbucket.profile.model.ProfileConfiguration;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.transform.csv.CsvTagMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Map;

/**
 * User: mike
 * Date: 27/04/14
 * Time: 4:36 PM
 */
public class TagMapper extends TagInputBean implements DelimitedMappable {
    private boolean hasHeader = true;
    public TagMapper(ImportProfile importProfile) {
        setLabel(importProfile.getDocumentType());
        hasHeader = importProfile.hasHeader();
    }

    @Override
    public ProfileConfiguration.ContentType getImporter() {
        return ProfileConfiguration.ContentType.CSV;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ProfileConfiguration.DataType getABType() {
        return ProfileConfiguration.DataType.TAG;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, Object> setData(String[] headerRow, String[] line, ProfileConfiguration staticDataResolver, FdReader dataResolver) throws JsonProcessingException {
        return null;
    }

    @Override
    public boolean hasHeader() {
        return true;
    }

    public static DelimitedMappable newInstance(ImportProfile importProfile) {
        if (importProfile.getContentType()== ProfileConfiguration.ContentType.CSV)
            return new CsvTagMapper();

        return new TagMapper(importProfile);
    }

    @Override
    public char getDelimiter() {
        return ',';  //To change body of implemented methods use File | Settings | File Templates.
    }
}
