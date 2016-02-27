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

package org.flockdata.transform.xml;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.flockdata.helper.FlockException;
import org.flockdata.profile.model.ContentProfile;
import org.flockdata.profile.model.Mappable;
import org.flockdata.track.bean.ContentInputBean;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * User: Mike Holdsworth
 * Since: 20/11/13
 */
public interface XmlMappable extends Mappable {
    /**
     *
     *
     * @return XML Data document type
     */

    @JsonIgnore
    String getDataType();

    ContentInputBean setXMLData(XMLStreamReader xsr, ContentProfile importProfile) throws JAXBException, JsonProcessingException, FlockException;

    /**
     * XML files require skipping.
     * Use this to position the reader to the collection of elements
     *
     * @param xsr stream
     * @throws XMLStreamException
     */
    void positionReader(XMLStreamReader xsr) throws XMLStreamException;

    XmlMappable newInstance(ContentProfile importProfile);
}

