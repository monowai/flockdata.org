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

package org.flockdata.transform.tags;

import org.flockdata.helper.FlockException;
import org.flockdata.profile.model.ContentProfile;
import org.flockdata.profile.model.Mappable;
import org.flockdata.registration.TagInputBean;
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

        return new TagMapper(contentProfile.getDocumentType().getName());
    }

}
