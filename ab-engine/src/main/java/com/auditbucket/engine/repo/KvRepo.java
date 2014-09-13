/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.engine.repo;

import com.auditbucket.track.model.Entity;

import java.io.IOException;

/**
 * User: Mike Holdsworth
 * Since: 31/01/14
 */
public interface KvRepo {
    public void add(Entity entity, Long key, byte[] what) throws IOException;

    public byte[] getValue(Entity entity, Long key);

    public void delete(Entity entity, Long key);

    public void purge(String index);

    String ping();
}
