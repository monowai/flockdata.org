package com.auditbucket.test.functional;

import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.track.bean.ContentInputBean;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.test.utils.Helper;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * User: mike
 * Date: 17/09/14
 * Time: 5:03 PM
 */

public class AttachmentTests extends EngineBase {
    @Test
    public void duplicate_sameContentIgnored() throws Exception{
        SystemUser su = registerSystemUser("duplicate_sameContentIgnored", mike_admin);
        FortressInputBean f = new FortressInputBean("attachmentFun", true);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), f);

        EntityInputBean entity = new EntityInputBean(fortress.getName(), "zippy", "blag", new DateTime(), "ABC");
        ContentInputBean content = new ContentInputBean("zippy", new DateTime());
        content.setAttachment(Helper.getPdfDoc(), "PdF", "testing.pdf");
        entity.setContent(content);
        TrackResultBean trackResult = mediationFacade.trackEntity(fortress, entity);
        assertFalse("This should have been the first entity logged", trackResult.entityExists());

        // Update without changing the content
        trackResult = mediationFacade.trackEntity(fortress, entity);
        assertTrue("Tracked the same file, so should have been ignored",trackResult.entityExists());
    }
}
