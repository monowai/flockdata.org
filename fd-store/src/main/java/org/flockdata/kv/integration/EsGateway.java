/*
 * Copyright (c) 2012-2015 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.kv.integration;

import org.flockdata.search.model.EsSearchResult;
import org.flockdata.search.model.QueryParams;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

/**
 * Interface to fd-search so support locating what data
 *
 * Created by mike on 22/03/15.
 */
@MessagingGateway
public interface EsGateway {

    @Retryable( maxAttempts = 5, backoff = @Backoff(delay = 600, multiplier = 5, random = true))
    @Gateway(requestChannel = "doDataQuery", replyChannel = "receiveContentReply")
    EsSearchResult getData(QueryParams queryParams);

}
