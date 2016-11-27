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

package org.flockdata.spring.bean;

import org.flockdata.track.bean.ContentInputBean;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * @author  nabil
 * @since 05/09/2013
 * To change this template use File | Settings | File Templates.
 */
public class EntityResultBeanTest {

    @Test
    public void testGetEntityLogInputBean() throws Exception {
        ContentInputBean contentInputBean = new ContentInputBean("", "auditKey", new DateTime(), null);
        EntityResultBean entityResultBean = new EntityResultBean(contentInputBean);
        Assert.assertEquals(entityResultBean.getKey(), "auditKey");
    }

    @Test
    public void testGetResultLogInputBean() throws Exception {
        ContentInputBean contentInputBean = new ContentInputBean("", "auditKey", new DateTime(), null);
        EntityResultBean entityResultBean = new EntityResultBean(contentInputBean);
        Assert.assertEquals(entityResultBean.getResult().getClass(), ContentInputBean.class);
    }
}
