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

package org.flockdata.kv.riak;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakFactory;
import com.basho.riak.client.bucket.Bucket;
import org.flockdata.kv.KvRepo;
import org.flockdata.kv.bean.KvContentBean;
import org.flockdata.track.bean.EntityBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class RiakRepo implements KvRepo {

    private static Logger logger = LoggerFactory.getLogger(RiakRepo.class);
    private IRiakClient client = null;
    private Lock lock = new ReentrantLock();

    private IRiakClient getClient() throws RiakException {
        if (client == null) {
            try {
                lock.lock();
                if (client == null) {
                    // ToDo: set server and host
                    client = RiakFactory.pbcClient();
                    client.generateAndSetClientId();

                }
            } finally{
                lock.unlock();
            }

        }
        return client;
    }

    public void add(KvContentBean contentBean) throws IOException {
        try {
            Bucket bucket = getClient().createBucket(getBucket(contentBean.getEntityBean())).execute();
            bucket.store(String.valueOf(contentBean.getLogId()), contentBean.getEntityContent()).execute();
        } catch (RiakException e) {
            logger.error("RIAK Repo Error", e);
            client.shutdown();
            client = null;
            throw new IOException("RIAK Repo Error [" + e.getMessage() + "]", e);
        }
    }

    private String getBucket(EntityBean entity) {
        return entity.getIndexName()+"/"+entity.getDocumentType().toLowerCase();
    }

    private String getBucket(Entity entity) {
        return entity.getFortress().getIndexName()+"/"+entity.getDocumentType().toLowerCase();
    }

    public byte[] getValue(EntityBean entity, Log forLog) {
        try {
            Bucket bucket = getClient().createBucket(getBucket(entity)).execute();
            IRiakObject result = bucket.fetch(String.valueOf(forLog.getId())).execute();
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

    public void delete(EntityBean entity, Log log) {
        try {
            Bucket bucket = getClient().fetchBucket(getBucket(entity)).execute();
            bucket.delete(String.valueOf(log.getId())).execute();
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
