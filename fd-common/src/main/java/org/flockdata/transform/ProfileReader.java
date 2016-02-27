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

package org.flockdata.transform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.profile.ContentProfileImpl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Handles ProfileConfiguration handling
 *
 * Created by mike on 13/01/16.
 */
public class ProfileReader {
    /**
     * Reads an ImportProfile JSON file and returns the Pojo
     *
     * @param profile Fully file name
     * @return initialized ImportProfile
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static ContentProfileImpl getImportProfile(String profile) throws IOException, ClassNotFoundException {
        ContentProfileImpl contentProfileImpl;
        ObjectMapper om = FdJsonObjectMapper.getObjectMapper();

        File fileIO = new File(profile);
        if (fileIO.exists()) {
            contentProfileImpl = om.readValue(fileIO, ContentProfileImpl.class);

        } else {
            InputStream stream = ClassLoader.class.getResourceAsStream(profile);
            if (stream != null) {
                contentProfileImpl = om.readValue(stream, ContentProfileImpl.class);
            } else {
                throw new IllegalArgumentException("Unable to locate the profile [" + profile +"] Working directory ["+System.getProperty("user.dir")+"]");
            }
        }
        return contentProfileImpl;
    }
}
