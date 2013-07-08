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

package com.auditbucket.core.service;

import com.auditbucket.registration.model.IFortress;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Mike Holdsworth
 * Date: 13/04/13
 * Time: 3:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class GitHandler {

    private GitHub github = null;
    private GHRepository repo;

    public void initHandler(IFortress fortress) {

        try {
            github = GitHub.connect();
            if (repo == null)
                repo = findRepo(fortress);


        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    public void deleteRepo(IFortress fortress) {
        try {
            repo.delete();

            if (repo != null)
                System.out.print("Failed to remove repo" + fortress.getFortressKey());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private GHRepository findRepo(IFortress fortress) throws IOException {

        try {
            return github.getRepository("monowai/" + fortress.getFortressKey());
        } catch (IOException e) {
            return createRepo(fortress);
        }
    }

    private GHRepository createRepo(IFortress fortress) throws IOException {
        String folder = "/Users/mike/git"; //fortress.getRepo();
        // Should only need to do this once, when registering a fortress

        repo = github.createRepository(
                fortress.getFortressKey(), "this is my new repository",
                "http://www.monowai.com/", true/*public*/);

        String user = "monowai";
        repo.addCollaborators(github.getUser(user));

        return repo;

    }
}
