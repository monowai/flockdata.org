/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

import org.flockdata.helper.FlockException;
import org.flockdata.profile.model.ContentProfile;
import org.flockdata.profile.model.Mappable;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.transform.csv.CsvTagMapper;

import java.util.Map;

/**
 * User: mike
 * Date: 27/04/14
 * Time: 4:36 PM
 */
public class TagMapper extends TagInputBean implements Mappable{

    public TagMapper(String documentName) {
        setLabel(documentName);
    }

    public Map<String, Object> setData(Map<String,Object>row, ContentProfile contentProfile) throws FlockException {
        return null;
    }

    public static Mappable newInstance(ContentProfile contentProfile) {
        if (contentProfile.getContentType()== ContentProfile.ContentType.CSV)
            return new CsvTagMapper();

        return new TagMapper(contentProfile.getDocumentName());
    }

}
