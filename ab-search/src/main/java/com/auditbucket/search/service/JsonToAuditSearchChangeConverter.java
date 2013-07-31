package com.auditbucket.search.service;

import com.auditbucket.search.AuditSearchChange;
import com.auditbucket.search.SearchResult;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Convert ByteArrays to String.
 */
@Component("jsonToAuditSearchChangeConverter")
public class JsonToAuditSearchChangeConverter extends SimpleMessageConverter {

    private static final String DEFAULT_CHARSET = "UTF-8";

    @Override
    public Object fromMessage(final Message message) throws MessageConversionException {

        final Object content = super.fromMessage(message);
        try {
            if (content instanceof String) {
                ObjectMapper mapper = new ObjectMapper();
                AuditSearchChange auditSearchChange = mapper.readValue(((String) content).getBytes(), AuditSearchChange.class);
                return auditSearchChange;
            }
        }
        catch (IOException e1) {
            throw new MessageConversionException("failed to convert text-based Message content", e1);
        }
        return content;
    }

}
