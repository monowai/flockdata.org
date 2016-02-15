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

    String searchBinding() {
        return searchBinding;
    }

    String searchExchange() {
        return searchExchange;
    }

    String storeBinding() {
        return storeBinding;
    }

    String storeExchange() {
        return storeExchange;
    }

    String trackBinding() {
        return trackBinding;
    }

    String trackExchange() {
        return trackExchange;
    }
}
