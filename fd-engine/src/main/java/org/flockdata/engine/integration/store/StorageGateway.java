package org.flockdata.engine.integration.store;

import org.flockdata.store.Store;
import org.flockdata.store.StoredContent;
import org.flockdata.store.bean.StorageBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Retryable;

/**
 * Created by mike on 20/02/16.
 */
@MessagingGateway
@Configuration
public interface StorageGateway {
    @Payload("new java.util.Date()")
    @Gateway(requestChannel = "storePing")
    String ping();

    @Gateway(requestChannel = "startStoreRead", requestTimeout = 40000, replyChannel = "storeReadResult")
    StoredContent read(Store store, String index, String type, String key);

    @Gateway(requestChannel = "startStoreWrite", requestTimeout = 40000, replyChannel = "nullChannel")
    @Retryable
//    @Async("fd-store")
    void write(StorageBean resultBean);

}
