package org.flockdata.search.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Created by mike on 12/02/16.
 */
@Configuration
public class Exchanges {
    @Value("${fd-search.binding:fd-search.binding}")
    String searchBinding;

    @Value("${fd-search.exchange:fd-search.exchange}")
    String searchExchange;

    @Value("${fd-store.binding:fd-store.binding}")
    String storeBinding;

    @Value("${fd-store.exchange:fd-store.exchange}")
    String storeExchange;

    @Value("${fd-track.binding:fd-track.binding}")
    String trackBinding;

    @Value("${fd-track.exchange:fd-track.exchange}")
    String trackExchange;

    @Value("${fd-engine.binding:fd.engine.binding}")
    private String engineBinding;

    @Value("${fd-engine.exchange:fd.engine.exchange}")
    private String engineExchange;

    public String getSearchBinding() {
        return searchBinding;
    }

    String getSearchExchange() {
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

    String getEngineBinding() {
        return engineBinding;
    }

    public String getEngineExchange() {
        return engineExchange;
    }
}
