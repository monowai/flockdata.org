/*
 *  Copyright 2012-2017 the original author or authors.
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

package org.flockdata.test.unit.client;

import org.flockdata.integration.ClientConfiguration;
import org.flockdata.integration.FdTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * To support unit testing. The entities and tags are not flushed so that the
 * transformed results can be retrieved and asserted in a test
 *
 * User can, and should, call reset() to clear down cached data which is handled automatically if your test
 * extends AbstractImport
 *
 * You can include AbstractImport in your project by including org.flockdata:fd-client:test-jar
 *
 * @tag Rest, Test, Integration
 * @author mholdsworth
 * @since 13/04/2016
 */
@Profile("dev")
@Service
public class FdTemplateMock extends FdTemplate {

    @Autowired
    public FdTemplateMock(ClientConfiguration clientConfiguration) {
      super(clientConfiguration);
    }

    /**
     * Prevents clearing down
     */
    @Override
    public void flush(){
        // Noop. Client can call reset() to clear cached data but otherwise we want to return
        //       transformed results
    }

}