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

package org.flockdata.track.bean;

import org.flockdata.model.EntityLog;
import org.flockdata.store.StoredContent;

/**
 * User: Mike Holdsworth
 * Since: 4/09/13
 */
public class LogDetailBean {

    private LogDetailBean() {
    }

    public LogDetailBean(EntityLog log, StoredContent what) {
        this();
        this.log = log;
        this.what = what;

    }

    private EntityLog log;
    private StoredContent what;

    public EntityLog getLog() {
        return this.log;
    }

    public StoredContent getWhat() {
        return this.what;
    }

}
