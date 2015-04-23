/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.kv;

import org.flockdata.kv.bean.KvContentBean;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.scheduling.annotation.Async;

/**
 * User: mike
 * Date: 19/11/14
 * Time: 11:48 AM
 */
@MessagingGateway (asyncExecutor = "fd-store")
@Async("fd-store")
public interface KvGateway {

    @Gateway(requestChannel = "startKvWrite", requestTimeout = 40000, replyChannel = "nullChannel")
    void doKvWrite(KvContentBean resultBean);
}
