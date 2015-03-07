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

package org.flockdata.track.model;

import org.flockdata.registration.model.FortressUser;

/**
 * User: Mike Holdsworth
 * Date: 15/04/13
 * Time: 5:49 AM
 */
public interface Log {

    String CREATE = "Create";
    String UPDATE = "Update";

    String getChecksum();

    void setChecksum(String checksum);

    public abstract FortressUser getWho();

    public String getComment();

    /**
     * optional comment
     *
     * @param comment searchable.
     */
    public void setComment(String comment);

    public void setTxRef(TxRef txRef);

    ChangeEvent getEvent();

    public boolean isCompressed();

    void setPreviousLog(Log previousChange);

    Log getPreviousLog();

    Long getId();

    /**
     * @return mechanism used to store the what text
     */
    public String getWhatStore();

    /**
     * defaults to Neo4j
     *
     * @param storage where to store
     */
    public void setWhatStore(String storage);

    void setEvent(ChangeEvent event);

    void setCompressed(Boolean compressed);

    EntityLog getEntityLog();

    void setContent(KvContent entityContent);

    public double getProfileVersion();

    KvContent getContent();

    void setEntityLog(EntityLog newLog);

    public String getContentType();

    public void setContentType(String contentType) ;

    public String getFileName() ;

    public void setFileName(String fileName);

}
