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

package org.flockdata.store.repos;

import org.flockdata.helper.ObjectHelper;
import org.flockdata.store.KvContent;
import org.flockdata.store.LogRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;

@Component
public class RedisRepo extends AbstractStore {

    @Autowired
    private RedisTemplate<Long, byte[]> template;
    private static Logger logger = LoggerFactory.getLogger(AbstractStore.class);

    public void add(KvContent kvContent) throws IOException {
        template.opsForValue().set(kvContent.getId(), ObjectHelper.serialize(kvContent.getContent()));
    }

    public KvContent getValue(LogRequest logRequest) {
        byte[] bytes = template.opsForValue().get(logRequest.getLogId());

        try {
            Object oResult = ObjectHelper.deserialize(bytes);
            return getKvContent(logRequest.getLogId(), oResult);
        } catch (ClassNotFoundException | IOException e) {
            logger.error("Error extracting content for " + logRequest, e);
        }
        return null;
    }

    public void delete(LogRequest logRequest) {
        template.opsForValue().getOperations().delete(logRequest.getLogId());
    }

    @Override
    public void purge(String index) {
        logger.debug("Purge not supported for REDIS. Ignoring this request");
    }

    @Override
    public String ping() {
        Date when = new Date();
        template.opsForValue().setIfAbsent(-99999l, when.toString().getBytes());
        template.opsForValue().getOperations().delete(-99999l);
        return "Redis is OK";
    }

}
