package org.flockdata.engine.integration;

import org.flockdata.search.model.*;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.handler.annotation.Payload;

/**
 * All fd-search requests go here
 *
 * Created by mike on 20/02/16.
 */
@MessagingGateway
public interface SearchGateway {

    @Payload("new java.util.Date()")
    @Gateway(requestChannel = "searchPing")
    String ping();

    @Gateway(requestChannel = "sendTagCloudRequest", replyChannel = "tagCloudResult")
    TagCloud getTagCloud(TagCloudParams tagCloudParams);

    @Gateway(requestChannel = "sendMetaKeyQuery", replyChannel = "metaKeyResult")
    MetaKeyResults metaKeys(QueryParams queryParams);

    @Gateway(requestChannel = "sendSearchRequest", replyChannel = "fdViewResult")
    EsSearchResult fdSearch(QueryParams queryParams);

    @Gateway(requestChannel = "sendEntityIndexRequest", replyChannel = "nullChannel", requestTimeout = 10000)
    void makeSearchChanges(EntitySearchChanges searchChanges);


}
