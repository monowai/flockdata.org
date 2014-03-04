package com.auditbucket.engine.repo.riak;

import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.engine.repo.KvRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.keyvalue.riak.core.RiakTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;

@Component
public class RiakRepo implements KvRepo {

    @Autowired
    private RiakTemplate riak;

    //private Logger logger = LoggerFactory.getLogger(RiakRepo.class);

    public void add(AuditHeader auditHeader, Long key, byte[] value) throws IOException {
        riak.setAsBytes(auditHeader.getIndexName(), key, value);
    }

    public byte[] getValue(AuditHeader auditHeader, Long key) {
        return riak.getAsBytes(auditHeader.getIndexName(), key);
    }

    public void delete(AuditHeader auditHeader, Long key) {
        riak.delete(auditHeader.getIndexName(), key);
    }

    @Override
    public String ping() {
        riak.setIfKeyNonExistent("ab.ping", "ping", new Date());
        riak.delete("ab.ping", "ping");
        return "Riak is OK";

    }
}
