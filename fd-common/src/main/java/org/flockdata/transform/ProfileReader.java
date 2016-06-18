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
import org.flockdata.profile.ContentModelImpl;

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
     * @param file Fully file name
     * @return initialized ImportProfile
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static ContentModelImpl getContentModel(String file) throws IOException, ClassNotFoundException {
        ContentModelImpl contentProfileImpl;
        ObjectMapper om = FdJsonObjectMapper.getObjectMapper();

        File fileIO = new File(file);
        if (fileIO.exists()) {
            contentProfileImpl = om.readValue(fileIO, ContentModelImpl.class);

        } else {
            InputStream stream = ClassLoader.class.getResourceAsStream(file);
            if (stream != null) {
                contentProfileImpl = om.readValue(stream, ContentModelImpl.class);
            } else {
                throw new IllegalArgumentException("Unable to locate the ContentModel [" + file +"] Working directory ["+System.getProperty("user.dir")+"]");
            }
        }
        return contentProfileImpl;
    }
}
