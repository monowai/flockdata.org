package org.flockdata.engine.integration;

import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.FlockException;
import org.flockdata.search.model.SearchResult;
import org.flockdata.search.model.SearchResults;
import org.flockdata.track.service.EntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;

/**
 * fd-search -->> fd-engine (inbound)
 *
 * Created by mike on 21/07/15.
 */
@Service
public class SearchRequests {
    private Logger logger = LoggerFactory.getLogger(SearchRequests.class);

    @Autowired
    EntityService entityService;

    //
    @ServiceActivator(inputChannel = "searchDocSyncResult", requiresReply = "false", adviceChain = {"fds.retry"})
    public void syncSearchResult(byte[] searchResults) throws IOException {
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
        Collection<SearchResult> theResults = searchResults.getSearchResults();
        int count = 0;
        int size = theResults.size();
        logger.debug("searchDocSyncResult processing {} incoming search results", size);
        for (SearchResult searchResult : theResults) {
            count++;
            logger.debug("Updating {}/{} from search metaKey =[{}]", count, size, searchResult);
            Long entityId = searchResult.getEntityId();
            if (entityId == null)
                return;

            try {
                entityService.recordSearchResult(searchResult, entityId);
            } catch (FlockException e) {
                logger.error("Unexpected error recording searchResult for entityId " + entityId, e);
            }
        }
        logger.trace("Finished processing search results");
    }

}
