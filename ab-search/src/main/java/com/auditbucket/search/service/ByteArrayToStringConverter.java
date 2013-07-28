package com.auditbucket.search.service;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;

/**
 * Convert ByteArrays to String.
 */
@Component("byteArrayToStringConverter")
public class ByteArrayToStringConverter extends SimpleMessageConverter {

    private static final String DEFAULT_CHARSET = "UTF-8";

    @Override
    public Object fromMessage(final Message message) throws MessageConversionException {
        final Object content = super.fromMessage(message);
        try {
            if (content instanceof byte[]) {
                return new String((byte[]) content, DEFAULT_CHARSET);
            }
        } catch (final UnsupportedEncodingException e) {
            throw new MessageConversionException("failed to convert text-based Message content", e);
        }
        return content;
    }

}
