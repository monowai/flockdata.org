package com.auditbucket.search.endpoint;

import com.auditbucket.search.model.MetaSearchChange;
import com.auditbucket.search.model.MetaSearchChanges;
import com.auditbucket.search.model.SearchResult;
import com.auditbucket.search.model.SearchResults;
import com.auditbucket.search.service.EngineGateway;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.SearchChange;
import com.auditbucket.track.model.TrackSearchDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Service;

/**
 * Services TRACK requests from the Engine
 * User: mike
 * Date: 12/04/14
 * Time: 6:23 AM
 */
@Service
@MessageEndpoint
public class TrackServiceEP {
    @Autowired
    private TrackSearchDao trackSearch;

    private Logger logger = LoggerFactory.getLogger(TrackServiceEP.class);

    @Autowired(required = false)
    private EngineGateway engineGateway;

    /**
     * Triggered by the Engine, this is the payload that is required to be indexed
     * <p/>
     * It may or may not already exist.
     *
     * @param changes to process
     */
    @ServiceActivator(inputChannel = "makeSearchRequest") // Subscriber
    public void createSearchableChange(MetaSearchChanges changes) {
        Iterable<MetaSearchChange> thisChange = changes.getChanges();
        logger.info("Received request to index Batch ");
        SearchResults results = new SearchResults();
        int processed = 0;
        for (SearchChange metaSearchChange : thisChange) {
            processed++;
            logger.trace("searchRequest received for {}", metaSearchChange);

            if (metaSearchChange.isDelete()) {
                logger.debug("Delete request");
                trackSearch.delete(metaSearchChange);
                return;
            }
            SearchResult result = new SearchResult(trackSearch.update(metaSearchChange));

            // Used to tie the fact that the doc was updated back to the engine
            result.setLogId(metaSearchChange.getLogId());
            result.setMetaId(metaSearchChange.getMetaId());
            if (metaSearchChange.isReplyRequired()) {
                results.addSearchResult(result);
                logger.trace("Dispatching searchResult to ab-engine {}", result);
            } else {
                logger.trace("No reply required");
            }

        }
        if (!results.isEmpty()) {
            logger.debug("Processed {} requests. Sending back {} SearchChanges", processed, results.getSearchResults().size());
            engineGateway.handleSearchResult(results);
        }

    }

    public void delete(MetaHeader metaHeader) {
        //trackDao.delete(metaHeader, null);
    }

    public byte[] findOne(MetaHeader header) {
        return null;
        //return trackDao.findOne(header);
    }

    public byte[] findOne(MetaHeader header, String id) {
        return null;
        //return trackDao.findOne(header, id);
    }

}
