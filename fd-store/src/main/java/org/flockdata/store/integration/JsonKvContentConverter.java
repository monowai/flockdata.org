/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.store.integration;

import java.io.IOException;
import org.flockdata.helper.JsonUtils;
import org.flockdata.store.bean.StorageBean;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.stereotype.Component;

/**
 * @author mholdsworth
 * @tag Store, Messaging, Json
 * @since 19/11/2014
 */
@Component("jsonToKvContentConverter")
public class JsonKvContentConverter extends SimpleMessageConverter {

  @Override
  public Object fromMessage(final Message message) throws MessageConversionException {

    try {
      return JsonUtils.toObject(message.getBody(), StorageBean.class);
    } catch (IOException e1) {
      e1.printStackTrace();
      throw new MessageConversionException("failed to convert text-based Message content", e1);
    }
  }
}
