package com.auditbucket.test.unit;

import com.auditbucket.audit.service.GitHandler;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.repo.neo4j.model.Company;
import com.auditbucket.registration.repo.neo4j.model.Fortress;

/**
 * Created with IntelliJ IDEA.
 * User: mike
 * Date: 13/04/13
 * Time: 3:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestGitHub {
    public void testConnect() {
        GitHandler gh = new GitHandler();
        IFortress fortress = new Fortress(new FortressInputBean("monowai"), new Company("Monowai Dev"));
        gh.initHandler(fortress);
        gh.deleteRepo(fortress);
    }
}
