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

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.RiakException;
import com.basho.riak.client.api.cap.Quorum;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.RiakNode;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.client.core.query.RiakObject;
import com.basho.riak.client.core.util.BinaryValue;
import org.flockdata.helper.ObjectHelper;
import org.flockdata.kv.AbstractKvRepo;
import org.flockdata.kv.FdKvConfig;
import org.flockdata.kv.bean.KvContentBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.KvContent;
import org.flockdata.track.model.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Component
public class RiakRepo extends AbstractKvRepo{

    private static Logger logger = LoggerFactory.getLogger(RiakRepo.class);
    private RiakClient client = null;
    @Autowired
    FdKvConfig kvConfig;

    private RiakClient getClient() throws RiakException, UnknownHostException {
        if (client!= null )
            return client;

        RiakNode.Builder builder = new RiakNode.Builder();
        builder.withMinConnections(10);
        builder.withMaxConnections(50);

        List<String> addresses = new LinkedList<>();

        addresses.add(kvConfig.getRiakUrl());

        List<RiakNode> nodes = RiakNode.Builder.buildNodes(builder, addresses);
        RiakCluster cluster = new RiakCluster.Builder(nodes).build();
        cluster.start();
        client = new RiakClient(cluster);
        return client;
    }

    public void add(KvContent kvContent) throws IOException {
        try {
            Namespace ns = new Namespace(bucketType, kvContent.getBucket());
            Location location = new Location(ns, kvContent.getId().toString());
            RiakObject riakObject = new RiakObject();
            byte[]bytes = ObjectHelper.serialize(kvContent.getContent());
            riakObject.setValue(BinaryValue.create(bytes));
            StoreValue store = new StoreValue.Builder(riakObject)
                    .withLocation(location)
                    .withOption(StoreValue.Option.W, new Quorum(3)).build();
            getClient().execute(store);


        } catch (RiakException|UnknownHostException|ExecutionException|InterruptedException e) {
            if (client != null) {
                client.shutdown();
                client = null;
            }
            throw new IOException("RIAK Repo Error [" + e.getMessage() + "]", e);
        }
    }

    static final String bucketType = "default";

    public KvContent getValue(Entity entity, Log forLog) {
        try {
            Namespace ns = new Namespace(bucketType, KvContentBean.parseBucket(entity));
            Location location = new Location(ns, forLog.getId().toString());
            FetchValue fv = new FetchValue.Builder(location).build();
            FetchValue.Response response = getClient().execute(fv);

            RiakObject result = response.getValue(RiakObject.class);

            if (result != null) {
                Object oResult = ObjectHelper.deserialize(result.getValue().getValue());
                return getKvContent(forLog, oResult);
            }
        } catch (InterruptedException|RiakException|ExecutionException|IOException|ClassNotFoundException e) {
            logger.error("KV Error", e);
            if (client != null) {
                client.shutdown();
                client = null;
            }
            return null;
        }
        return null;
    }

    public void delete(Entity entity, Log log) {
        try {
            Namespace ns = new Namespace(bucketType, KvContentBean.parseBucket(entity));
            Location location = new Location(ns, log.getId().toString());
            DeleteValue dv = new DeleteValue.Builder(location).build();
            getClient().execute(dv);
        } catch (UnknownHostException|RiakException|InterruptedException|ExecutionException e) {
            logger.error("RIAK Repo Error", e);
            client.shutdown();
            client = null;
        }

    }

    public void purge(String bucket) {
//        try {
//            Namespace ns = new Namespace("default", bucket);
//
//            getClient().reset(bucket);
//        } catch (RiakException|UnknownHostException e) {
//            logger.error("RIAK Repo Error", e);
//            client.shutdown();
//            client = null;
//        }

    }

    @Override
    public String ping() {
//        try {
//            getClient().ping();
//        } catch (RiakException e) {
//            return "Error pinging RIAK";
//        }
        return "Pinging Riak not yet supported";

    }
}
