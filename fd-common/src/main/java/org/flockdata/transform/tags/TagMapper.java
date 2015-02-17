/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.transform.tags;

import org.flockdata.profile.ImportProfile;
import org.flockdata.profile.model.ProfileConfiguration;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.transform.DelimitedMappable;
import org.flockdata.transform.csv.CsvTagMapper;
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
        setLabel(importProfile.getDocumentName());
        hasHeader = importProfile.hasHeader();
    }

    @Override
    public ProfileConfiguration.ContentType getImporter() {
        return ProfileConfiguration.ContentType.CSV;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, Object> setData(String[] headerRow, String[] line, ProfileConfiguration profileConfiguration) throws JsonProcessingException {
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
