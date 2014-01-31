package com.auditbucket.engine.repo.redis;

import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.engine.repo.KvRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class RedisRepo implements KvRepo {

    @Autowired
    private RedisTemplate<Long, byte[]> template;

    public void add(AuditHeader auditHeader, Long key, byte[] value) {
        template.opsForValue().set(key, value);
    }

    public byte[] getValue(AuditHeader auditHeader, Long key) {
        return template.opsForValue().get(key);
    }

    public void delete(AuditHeader auditHeader, Long key) {
        template.opsForValue().getOperations().delete(key);
    }

    @Override
    public String ping() {
        Date when = new Date();
        template.opsForValue().setIfAbsent(-99999l, when.toString().getBytes());
        template.opsForValue().getOperations().delete(-99999l);
        return "Redis is OK";
    }
}
