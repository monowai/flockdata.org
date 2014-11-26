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

package org.flockdata.search.service;

import org.flockdata.helper.CompressionHelper;
import org.flockdata.helper.FlockDataJsonFactory;
import org.flockdata.search.model.EntitySearchChanges;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Convert incoming bytes to an ArrayList<SearchChange>
 *
 */
@Component("jsonToSearchChangeConverter")
public class JsonSearchChangeConverter extends SimpleMessageConverter {

    @Override
    public Object fromMessage(final Message message) throws MessageConversionException {

        final Object content = super.fromMessage(message);
        try {
            if (content instanceof String) {
                return FlockDataJsonFactory.getObjectMapper().readValue(((String) content).getBytes(CompressionHelper.charSet), EntitySearchChanges.class);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
            throw new MessageConversionException("failed to convert text-based Message content", e1);
        }
        return content;
    }

}
