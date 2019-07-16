/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

package org.flockdata.test.engine.unit;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.util.Map;
import org.flockdata.engine.query.service.EsHelper;
import org.flockdata.helper.JsonUtils;
import org.junit.Test;

/**
 * @author Mike Holdsworth
 * @since 24/09/17
 */
public class TestDataExtraction {

    @Test
    public void extractMap() throws Exception {
        String json = "{\"took\":8,\"timed_out\":false,\"_shards\":{\"total\":5,\"successful\":5,\"failed\":0},\"hits\":{\"total\":1,\"max_score\":1.3862944,\"hits\":[{\"_index\":\"fd.testcompany.suppressversionsonbydocbasis.something\",\"_type\":\"something\",\"_id\":\"Sun Sep 24 16:36:15 SGT 2017\",\"_score\":1.3862944,\"_source\":{\"fortress\":\"suppressVersionsOnByDocBasis\",\"whenCreated\":1506242215773,\"code\":\"Sun Sep 24 16:36:15 SGT 2017\",\"data\":{\"value\":\"alpha\"},\"whenUpdated\":1506242220034,\"lastEvent\":\"Create\",\"description\":null,\"key\":\"9boqDCB8QRCi_Qxq7AEHNQ\",\"timestamp\":1506242220047}}]}}";
        Map<String, Object> jsonMap = JsonUtils.toMap(json);
        Map<String, Object> extractedData = new EsHelper().extractData(jsonMap);
        assertNotNull(extractedData);
        assertTrue(extractedData.containsKey("data"));
    }
}
