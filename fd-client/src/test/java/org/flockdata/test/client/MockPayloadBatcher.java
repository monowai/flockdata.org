/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.test.client;

import org.flockdata.shared.FdBatcher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * To support unit testing. The entities and tags are not flushed so that the
 * transformed results can be validated by a test
 *
 * User must call reset() to clear down any cached data
 *
 * Created by mike on 13/04/16.
 */
@Profile("dev")
@Service
public class MockPayloadBatcher extends FdBatcher {

    @Override
    public void flush(){
        // Noop
    }
}