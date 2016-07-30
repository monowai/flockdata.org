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

package org.flockdata.client.commands;

import org.flockdata.client.FdTemplate;

/**
 * Commands are immutable and re-usable. Ensure your clientConfiguration has been configured prior
 * to object creation
 *
 * Created by mike on 13/04/16.
 */
abstract class AbstractRestCommand implements Command {

    String error = null;
    FdTemplate fdTemplate;

    /**
     * Set's the basic immutable properties for this command
     *
     * @param fdTemplate Helper class to access HTTP resources
     */
    AbstractRestCommand(FdTemplate fdTemplate) {
        this.fdTemplate = fdTemplate;
    }

    public String getUrl(){
        return fdTemplate.getUrl();
    }

    // NULL if no error
    public String error() {
        return error;
    }

    public boolean worked() {
        return error == null;
    }
}
