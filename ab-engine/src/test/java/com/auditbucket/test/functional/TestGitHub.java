/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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

package com.auditbucket.test.functional;

import com.auditbucket.engine.service.GitHandler;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.repo.neo4j.model.CompanyNode;
import com.auditbucket.registration.repo.neo4j.model.FortressNode;

/**
 * Created with IntelliJ IDEA.
 * User: Mike Holdsworth
 * Date: 13/04/13
 * Time: 3:56 PM
 * To change this template use File | Settings | File Templates.
 */
class TestGitHub {
    public void testConnect() {
        GitHandler gh = new GitHandler();
        Fortress fortress = new FortressNode(new FortressInputBean("monowai"), new CompanyNode("Monowai Dev"));
        //gh.initHandler(fortress);
        //gh.deleteRepo(fortress);
    }
}
