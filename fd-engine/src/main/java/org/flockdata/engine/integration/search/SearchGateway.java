package org.flockdata.engine.integration.search;

import org.flockdata.search.model.*;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.handler.annotation.Payload;

/**
 * All fd-search requests go here
 * <p>
 * Created by mike on 20/02/16.
 */
@MessagingGateway
public interface SearchGateway {

    //ToDo: I like gateway methods being encapsulated here, but you can only have one @Retryable annotation
    //      so we should move these into their respective Request classes to support retry semantics
    @Payload("new java.util.Date()")
    @Gateway(requestChannel = "searchPing")
    String ping();

    @Gateway(requestChannel = "sendTagCloudRequest", replyChannel = "tagCloudResult")
    TagCloud getTagCloud(TagCloudParams tagCloudParams);

    @Gateway(requestChannel = "sendMetaKeyQuery", replyChannel = "metaKeyResult")
    MetaKeyResults metaKeys(QueryParams queryParams);

    @Gateway(requestChannel = "sendSearchRequest", replyChannel = "fdViewResult")
    EsSearchResult fdSearch(QueryParams queryParams);

    @Gateway(requestChannel = "sendEntityIndexRequest", replyChannel = "nullChannel")
//    @Retryable
    void makeSearchChanges(EntitySearchChanges searchChanges);



}
