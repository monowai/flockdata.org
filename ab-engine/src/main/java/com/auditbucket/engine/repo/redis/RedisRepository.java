package com.auditbucket.engine.repo.redis;

import com.auditbucket.audit.model.AuditWhat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisRepository {

    @Autowired
    private RedisTemplate<Long, AuditWhat> template;

    public void add(Long key, AuditWhat value) {
        template.opsForValue().set(key, value);
    }

    public AuditWhat getValue(Long key) {
        return template.opsForValue().get(key);
    }

    public void delete(Long key) {
        template.opsForValue().getOperations().delete(key);
    }
}
