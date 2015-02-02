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

package org.flockdata.transform.xml;


import org.flockdata.helper.FlockException;
import org.flockdata.profile.model.Mappable;
import org.flockdata.profile.model.ProfileConfiguration;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.transform.FdReader;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;

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
    public String getDataType();

    XmlMappable newInstance(boolean simulateOnly);

    public ContentInputBean setXMLData(XMLStreamReader xsr, ProfileConfiguration importProfile, FdReader FdReader) throws JAXBException, JsonProcessingException, FlockException;

    /**
     * Some XML files require skipping.
     * Use this to position the reader to the collection of elements
     *
     * @param xsr stream
     * @throws XMLStreamException
     */
    void positionReader(XMLStreamReader xsr) throws XMLStreamException;
}

