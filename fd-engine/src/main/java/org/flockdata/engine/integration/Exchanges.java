package org.flockdata.engine.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Created by mike on 12/02/16.
 */
@Configuration
public class Exchanges {
    @Value("${fd-search.binding}")
    String searchBinding;

    @Value("${fd-search.exchange}")
    String searchExchange;

    @Value("${fd-store.binding}")
    String storeBinding;

    @Value("${fd-store.exchange}")
    String storeExchange;

    @Value("${fd-track.binding}")
    String trackBinding;

    @Value("${fd-track.exchange}")
    String trackExchange;

    public String getSearchBinding() {
        return searchBinding;
    }

    public String getSearchExchange() {
        return searchExchange;
    }

    public String getStoreBinding() {
        return storeBinding;
    }

    public String getStoreExchange() {
        return storeExchange;
    }

    public String getTrackBinding() {
        return trackBinding;
    }

    public String getTrackExchange() {
        return trackExchange;
    }
}
