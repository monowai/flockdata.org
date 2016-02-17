package org.flockdata.engine.integration;

import org.flockdata.store.bean.KvContentBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.retry.annotation.Retryable;

/**
 * Created by mike on 17/02/16.
 */
@IntegrationComponentScan
@Configuration
@Profile({"integration","production"})
public class StorageWriter {

    @MessagingGateway(asyncExecutor ="fd-store")
    @IntegrationComponentScan
    public interface StorageGateway {
        @Gateway(requestChannel = "startKvWrite", requestTimeout = 40000, replyChannel = "nullChannel")
        @Retryable
        void doStoreWrite(KvContentBean resultBean);
    }

}
