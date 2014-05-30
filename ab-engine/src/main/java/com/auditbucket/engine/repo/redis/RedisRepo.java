/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.engine.repo.redis;

import com.auditbucket.engine.repo.KvRepo;
import com.auditbucket.track.model.MetaHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class RedisRepo implements KvRepo {

    @Autowired
    private RedisTemplate<Long, byte[]> template;
    private static Logger logger = LoggerFactory.getLogger(RedisRepo.class);

    public void add(MetaHeader metaHeader, Long key, byte[] what) {
        template.opsForValue().set(key, what);
    }

    public byte[] getValue(MetaHeader metaHeader, Long key) {
        return template.opsForValue().get(key);
    }

    public void delete(MetaHeader metaHeader, Long key) {
        template.opsForValue().getOperations().delete(key);
    }

    @Override
    public void purge(String index) {
        logger.debug("Purge not supported for REDIS. Ignoring this request") ;
    }

    @Override
    public String ping() {
        Date when = new Date();
        template.opsForValue().setIfAbsent(-99999l, when.toString().getBytes());
        template.opsForValue().getOperations().delete(-99999l);
        return "Redis is OK";
    }
}
