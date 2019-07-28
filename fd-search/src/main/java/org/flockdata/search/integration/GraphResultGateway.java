package org.flockdata.search.integration;

import org.flockdata.search.SearchResults;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;

/**
 * @author mikeh
 * @since 2018-12-13
 */
@MessagingGateway
public interface GraphResultGateway {

  @Gateway(requestChannel = "searchReply", requestTimeout = 40000)
  void writeSearchResult(SearchResults searchResult);

}
