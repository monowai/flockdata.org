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

package com.auditbucket.engine.repo.riak;

import com.auditbucket.engine.repo.KvRepo;
import com.auditbucket.track.model.Entity;
import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakFactory;
import com.basho.riak.client.bucket.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RiakRepo implements KvRepo {

    private static Logger logger = LoggerFactory.getLogger(RiakRepo.class);
    private IRiakClient client = null;
    private final Object synLock = "RiakRepoLock";

    private IRiakClient getClient() throws RiakException {
        if (client == null) {
            synchronized (synLock) {
                if (client == null) {
                    // ToDo: set server and host
                    client = RiakFactory.pbcClient();
                    client.generateAndSetClientId();

                }
            }

        }
        return client;
    }

    public void add(Entity entity, Long key, byte[] what) throws IOException {
        try {
            Bucket bucket = getClient().createBucket(entity.getIndexName()).execute();
            bucket.store(String.valueOf(key), what).execute();
        } catch (RiakException e) {
            logger.error("RIAK Repo Error", e);
            client.shutdown();
            client = null;
            throw new IOException("RIAK Repo Error [" + e.getMessage() + "]", e);
        }
    }

    public byte[] getValue(Entity entity, Long key) {
        try {
            Bucket bucket = getClient().createBucket(entity.getIndexName()).execute();
            IRiakObject result = bucket.fetch(String.valueOf(key)).execute();
            if (result != null)
                return result.getValue();
        } catch (RiakException e) {
            logger.error("KV Error", e);
            if (client != null) {
                client.shutdown();
                client = null;
            }
            return null;
        }
        return null;
    }

    public void delete(Entity entity, Long key) {
        try {
            Bucket bucket = getClient().fetchBucket(entity.getIndexName()).execute();
            bucket.delete(String.valueOf(key)).execute();
        } catch (RiakException e) {
            logger.error("RIAK Repo Error", e);
            client.shutdown();
            client = null;
        }

    }

    public void purge(String index) {
        try {
            getClient().resetBucket(index);
        } catch (RiakException e) {
            logger.error("RIAK Repo Error", e);
            client.shutdown();
            client = null;
        }

    }

    @Override
    public String ping() {
        try {
            getClient().ping();
        } catch (RiakException e) {
            return "Error pinging RIAK";
        }
        return "Riak is OK";

    }
}
