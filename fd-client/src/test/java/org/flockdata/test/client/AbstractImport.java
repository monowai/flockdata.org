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

package org.flockdata.test.client;

import junit.framework.TestCase;
import org.flockdata.client.Configure;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.bean.SystemUserResultBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Tag;
import org.flockdata.track.bean.CrossReferenceInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FdWriter;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Created by mike on 12/02/15.
 */
public class AbstractImport {
    // You will likely want to reimplement this in order to validate your data in the flush routines

    static FdWriter fdWriter = new FdWriter() {
        @Override
        public SystemUserResultBean me() {
            return null;
        }

        @Override
        public  String flushTags(List<TagInputBean> tagInputBeans) throws FlockException {
            return null;
        }

        @Override
        public String flushEntities(Company company, List<EntityInputBean> entityBatch, ClientConfiguration configuration) throws FlockException {
            return null;
        }

        @Override
        public int flushXReferences(List<CrossReferenceInputBean> referenceInputBeans) throws FlockException {
            return 0;
        }

        @Override
        public boolean isSimulateOnly() {
            // Setting this to true will mean that the flush routines above are not called
            return false;
        }

        @Override
        public Collection<Tag> getCountries() throws FlockException {
            return null;
        }

        @Override
        public void close() {

        }
    };

    static ClientConfiguration getClientConfiguration(String jsonConfig) {
        File file = new File(jsonConfig);
        ClientConfiguration configuration = Configure.readConfiguration(file);
        TestCase.assertNotNull(configuration);
        if ( configuration.getDefaultUser() == null )
            configuration.setDefaultUser("test");
        return configuration;
    }
}
