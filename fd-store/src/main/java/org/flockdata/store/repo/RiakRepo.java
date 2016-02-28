/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

package org.flockdata.store.repo;

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
import org.flockdata.shared.IndexManager;
import org.flockdata.store.AbstractStore;
import org.flockdata.store.LogRequest;
import org.flockdata.store.StoredContent;
import org.flockdata.store.service.FdStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Component
@Profile("riak")
public class RiakRepo extends AbstractStore {

    private static Logger logger = LoggerFactory.getLogger(RiakRepo.class);
    private RiakClient client = null;
    @Autowired
    IndexManager indexManager;
    @Autowired
    FdStoreConfig kvConfig;

    private RiakClient getClient() throws RiakException, UnknownHostException {
        if (client != null)
            return client;

        RiakNode.Builder builder = new RiakNode.Builder();
        builder.withMinConnections(10);
        builder.withMaxConnections(50);

        List<String> addresses = Arrays.asList(kvConfig.riakHosts().split("\\s*,\\s*"));

        List<RiakNode> nodes = RiakNode.Builder.buildNodes(builder, addresses);
        RiakCluster cluster = new RiakCluster.Builder(nodes).build();
        cluster.start();
        client = new RiakClient(cluster);
        return client;
    }

    public void add(StoredContent storedContent) throws IOException {
        try {
            Namespace ns = new Namespace(bucketType, indexManager.toStoreIndex(storedContent.getEntity()));
            Location location = new Location(ns, storedContent.getId().toString());
            RiakObject riakObject = new RiakObject();
            byte[] bytes = ObjectHelper.serialize(storedContent.getContent());
            riakObject.setValue(BinaryValue.create(bytes));
            StoreValue store = new StoreValue.Builder(riakObject)
                    .withLocation(location)
                    .withOption(StoreValue.Option.W, new Quorum(3)).build();
            getClient().execute(store);


        } catch (RiakException | UnknownHostException | ExecutionException | InterruptedException e) {
            if (client != null) {
                client.shutdown();
                client = null;
            }
            throw new IOException("RIAK Repo Error [" + e.getMessage() + "]", e);
        }
    }

    @Override
    public StoredContent read(String index, String type, String id) {
        try {
            Namespace ns = new Namespace(bucketType, index);
            Location location = new Location(ns, id.toString());
            FetchValue fv = new FetchValue.Builder(location).build();
            FetchValue.Response response = getClient().execute(fv);

            RiakObject result = response.getValue(RiakObject.class);

            if (result != null) {
                Object oResult = ObjectHelper.deserialize(result.getValue().getValue());
                return getContent(id, oResult);
            }
        } catch (InterruptedException | RiakException | ExecutionException | IOException | ClassNotFoundException e) {
            logger.error("KV Error", e);
            if (client != null) {
                client.shutdown();
                client = null;
            }
            return null;
        }
        return null;
    }

    static final String bucketType = "default";

    public StoredContent read(LogRequest logRequest) {
        String index = indexManager.toStoreIndex(logRequest.getEntity());
        return read(index, bucketType, logRequest.getLogId().toString());
    }

    public void delete(LogRequest logRequest) {
        try {
            Namespace ns = new Namespace(bucketType, indexManager.toStoreIndex(logRequest.getEntity()));
            Location location = new Location(ns, logRequest.getLogId().toString());
            DeleteValue dv = new DeleteValue.Builder(location).build();
            getClient().execute(dv);
        } catch (UnknownHostException | RiakException | InterruptedException | ExecutionException e) {
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

    @PostConstruct
    void status(){
        Logger logger = LoggerFactory.getLogger("configuration");
        logger.info("**** Deployed Riak repo manager");
    }

}
