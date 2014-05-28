package com.auditbucket.search.service;

import com.auditbucket.search.model.JsonSearchChange;
import com.auditbucket.search.model.MetaSearchChanges;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Convert incoming bytes to an ArrayList<MetaSearchChange>
 *
 */
@Component("jsonToSearchChangeConverter")
public class JsonSearchChangeConverter extends SimpleMessageConverter {

    @Override
    public Object fromMessage(final Message message) throws MessageConversionException {

        final Object content = super.fromMessage(message);
        try {
            if (content instanceof String) {
                ObjectMapper mapper = new ObjectMapper();
                SimpleModule iModule = new SimpleModule("ImportParameters", new Version(1,0,0,null))
                        .addDeserializer(MetaSearchChanges.class, new JsonSearchChange());
                mapper.registerModule(iModule);

                return mapper.readValue(((String) content).getBytes(), MetaSearchChanges.class);
                        //TypeFactory typeFactory = mapper.getTypeFactory();
                //CollectionLikeType collectionType = typeFactory.constructCollectionType(ArrayList.class, MetaSearchChange.class);
                //return mapper.readValue(((String) content).getBytes(), collectionType);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
            throw new MessageConversionException("failed to convert text-based Message content", e1);
        }
        return content;
    }

}
