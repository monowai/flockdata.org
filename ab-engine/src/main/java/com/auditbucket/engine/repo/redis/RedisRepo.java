package com.auditbucket.engine.repo.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisRepo {

    @Autowired
    private RedisTemplate<Long, byte[]> template;

    public void add(Long key, byte[] value) {
        template.opsForValue().set(key, value);
    }

    public byte[] getValue(Long key) {
        return template.opsForValue().get(key);
    }

    public void delete(Long key) {
        template.opsForValue().getOperations().delete(key);
    }
}
