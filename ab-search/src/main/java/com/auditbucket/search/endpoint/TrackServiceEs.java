package com.auditbucket.search.endpoint;

import com.auditbucket.search.model.EntitySearchChange;
import com.auditbucket.search.model.EntitySearchChanges;
import com.auditbucket.search.model.SearchResult;
import com.auditbucket.search.model.SearchResults;
import com.auditbucket.search.service.EngineGateway;
import com.auditbucket.search.service.TrackService;
import com.auditbucket.track.model.Entity;
import com.auditbucket.track.model.SearchChange;
import com.auditbucket.track.model.TrackSearchDao;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * Services TRACK requests from the Engine
 * User: mike
 * Date: 12/04/14
 * Time: 6:23 AM
 */
@Service
@Transactional
@MessageEndpoint
public class TrackServiceEs implements TrackService {
    @Autowired
    private TrackSearchDao trackSearch;

    private Logger logger = LoggerFactory.getLogger(TrackServiceEs.class);

    @Autowired(required = false)
    private EngineGateway engineGateway;

    static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Triggered by the Engine, this is the payload that is required to be indexed
     * <p/>
     * It may or may not already exist.
     *
     * @param changes to process
     * @throws java.io.IOException if there is a problem with mapping files. This exception will keep
     * the message on the queue until the mapping is fixed
     */
    @Override
    public void createSearchableChange(EntitySearchChanges changes) throws IOException {
        Iterable<EntitySearchChange> thisChange = changes.getChanges();
        logger.debug("Received request to index Batch {}", changes.getChanges().size());
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
            result.setMetaId(metaSearchChange.getEntityId());
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
    @Override
    @ServiceActivator(inputChannel = "syncSearchDocs") // Subscriber
    public void createSearchableChange(byte[] bytes) throws IOException {
        createSearchableChange(objectMapper.readValue(bytes, EntitySearchChanges.class));

    }


        @Override
    public void delete(Entity entity) {
        //trackDao.delete(entity, null);
    }

    @Override
    public byte[] findOne(Entity entity) {
        return null;
        //return trackDao.findOne(entity);
    }

    @Override
    public byte[] findOne(Entity entity, String id) {
        return null;
        //return trackDao.findOne(entity, id);
    }

}
