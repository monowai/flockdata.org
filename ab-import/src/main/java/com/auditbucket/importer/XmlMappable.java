package com.auditbucket.importer;


import com.auditbucket.helper.AuditException;
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
    @JsonIgnore
    public String getDataType();

    XmlMappable newInstance();

    public String setXMLData(XMLStreamReader xsr) throws JAXBException, JsonProcessingException, AuditException;

    /**
     * Some XML files require skipping.
     * Use this to position the reader to the collection of elements
     *
     * @param xsr stream
     * @throws XMLStreamException
     */
    void positionReader(XMLStreamReader xsr) throws XMLStreamException;
}

