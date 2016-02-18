package org.flockdata.engine.integration;

import org.flockdata.store.bean.KvContentBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;

/**
 * Created by mike on 17/02/16.
 */
@IntegrationComponentScan
@Configuration
@Profile({"integration","production"})
public class StorageWriter {

    // Over AMQP

    @MessagingGateway
    @Profile({"integration","production"})
    public interface WriteStorageGateway {
        @Gateway(requestChannel = "startKvWrite", requestTimeout = 40000, replyChannel = "nullChannel")
        @Retryable
        @Async("fd-store")
        void doStoreWrite(KvContentBean resultBean);
    }

}
