package com.auditbucket.search.service;

import com.auditbucket.helper.CompressionHelper;
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
import java.nio.charset.Charset;

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

                return mapper.readValue(((String) content).getBytes(CompressionHelper.charSet), MetaSearchChanges.class);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
            throw new MessageConversionException("failed to convert text-based Message content", e1);
        }
        return content;
    }

}
