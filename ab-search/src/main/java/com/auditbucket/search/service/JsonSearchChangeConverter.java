package com.auditbucket.search.service;

import com.auditbucket.search.model.MetaSearchChange;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Convert incoming bytes to an ArrayList<MetaSearchChange>
 *
 */
@Component("jsonToAuditSearchChangeConverter")
public class JsonSearchChangeConverter extends SimpleMessageConverter {

    @Override
    public Object fromMessage(final Message message) throws MessageConversionException {

        final Object content = super.fromMessage(message);
        try {
            if (content instanceof String) {
                ObjectMapper mapper = new ObjectMapper();
                TypeFactory typeFactory = mapper.getTypeFactory();
                CollectionLikeType collectionType = typeFactory.constructCollectionType(ArrayList.class, MetaSearchChange.class);

                return mapper.readValue(((String) content).getBytes(), collectionType);
            }
        } catch (IOException e1) {
            throw new MessageConversionException("failed to convert text-based Message content", e1);
        }
        return content;
    }

}
