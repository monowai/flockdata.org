/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.engine;

import org.flockdata.shared.FdBatchWriter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Unit testing of fd-client requires holding on to payloads.
 * fd-engine unit testing wants to process them.
 * This class is to signal that fd-engine can use the FdBatcher in all its glory
 *
 *
 * Created by mike on 13/04/16.
 */
@Profile("dev")
@Service
public class MockFdBatchWriter extends FdBatchWriter {
}
