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

import org.flockdata.profile.ContentModelDeserializer;
import org.flockdata.profile.model.ContentModel;

import java.io.IOException;

/**
 * Handles ProfileConfiguration handling
 *
 * Created by mike on 13/01/16.
 */
@Deprecated
public class ProfileReader {
    /**
     * Reads an ImportProfile JSON file and returns the Pojo
     *
     * @param file Fully file name
     * @return initialized ImportProfile
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static ContentModel getContentModel(String file) throws IOException, ClassNotFoundException {
        return ContentModelDeserializer.getContentModel(file);
    }
}
