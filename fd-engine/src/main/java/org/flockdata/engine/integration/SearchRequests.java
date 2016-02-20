package org.flockdata.engine.integration;

import com.google.common.net.MediaType;
import org.flockdata.engine.track.service.SearchHandler;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.JsonUtils;
import org.flockdata.search.model.SearchResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * fd-search -->> fd-engine (inbound)
 *
 * Created by mike on 21/07/15.
 */
@Service
@Profile({"integration","production"})
public class SearchRequests {

    @Autowired
    SearchHandler searchHandler;

    @Autowired
    FdSearchChannels channels;

    private ObjectToJsonTransformer transformer;

    @PostConstruct
    public void createTransformer() {
        transformer = new ObjectToJsonTransformer(
                new Jackson2JsonObjectMapper(JsonUtils.getMapper())
        );
        transformer.setContentType(MediaType.JSON_UTF_8.toString());
        //return transformer;
    }

    public ObjectToJsonTransformer getTransformer(){
        return transformer;
    }

    //
    @ServiceActivator(inputChannel = "searchDocSyncResult", requiresReply = "false")
    @Retryable
    public void syncSearchResult(byte[] searchResults) throws IOException {
        // ToDo: Which of these two methods do we want??
        syncSearchResult(FdJsonObjectMapper.getObjectMapper().readValue(searchResults, SearchResults.class));
    }

    /**
     * Callback handler that is invoked from fd-search. This routine ties the generated search document ID
     * to the Entity
     * <p/>
     * ToDo: On completion of this, an outbound message should be posted so that the caller can be made aware(?)
     *
     * @param searchResults contains keys to tie the search to the entity
     */
    @ServiceActivator(inputChannel = "searchSyncResult", requiresReply = "false")
    public void syncSearchResult(SearchResults searchResults) {
        searchHandler.handlResults(searchResults);
    }


}
