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

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by mike on 29/06/15.
 */
public class EntityResults {

    Collection<TrackResultBean> trackResults = new ArrayList<>();

    public void addResult(TrackResultBean trackResult){
        if ( !trackResults.contains(trackResult))
            trackResults.add(trackResult);
    }

    public Collection<TrackResultBean> getTrackResults() {
        return trackResults;
    }


    public TrackResultBean getSingleResult() {
        if ( trackResults.isEmpty())
            return null;
        return getTrackResults().iterator().next();
    }
}
