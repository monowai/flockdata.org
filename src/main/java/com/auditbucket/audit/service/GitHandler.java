package com.auditbucket.audit.service;

import com.auditbucket.registration.model.IFortress;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: mike
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
                System.out.print("Failed to remove repo" + fortress.getUUID().toString());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private GHRepository findRepo(IFortress fortress) throws IOException {

        try {
            return github.getRepository("monowai/" + fortress.getUUID().toString());
        } catch (IOException e) {
            return createRepo(fortress);
        }
    }

    private GHRepository createRepo(IFortress fortress) throws IOException {
        String folder = "/Users/mike/git"; //fortress.getRepo();
        // Should only need to do this once, when registering a fortress

        repo = github.createRepository(
                fortress.getUUID().toString(), "this is my new repository",
                "http://www.monowai.com/", true/*public*/);

        String user = fortress.getUsers();
        repo.addCollaborators(github.getUser(user));

        return repo;

    }
}
