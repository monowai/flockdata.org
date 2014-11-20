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

package org.flockdata.kv.bean;

import org.flockdata.track.bean.EntityBean;
import org.flockdata.track.bean.TrackResultBean;

/**
 * User: mike
 * Date: 19/11/14
 * Time: 2:41 PM
 */
public class KvContentBean {
    private byte[] entityContent;
    private Long logId;

    public EntityBean getEntityBean() {
        return entityBean;
    }

    private EntityBean entityBean;

    public KvContentBean() {
    }

    public KvContentBean(TrackResultBean trackResultBean) {
        this();
        this.entityBean = new EntityBean(trackResultBean.getEntity());
        if (trackResultBean.getLogResult().getWhatLog() != null) {
            this.logId = trackResultBean.getLogResult().getWhatLog().getId();
            this.entityContent = trackResultBean.getLogResult().getWhatLog().getEntityContent();
        }
        trackResultBean.getEntity().getMetaKey();
    }


    public byte[] getEntityContent() {
        return entityContent;
    }

    public void setEntityContent(byte[] entityContent) {
        this.entityContent = entityContent;
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }
}
