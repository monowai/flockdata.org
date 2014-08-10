package com.auditbucket.test.functional;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.MetaHeader;
import junit.framework.Assert;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * User: Mike Holdsworth
 * Date: 15/04/13
 * Time: 6:43 AM
 */
@Transactional
public class TrackAPIKeys extends TestEngineBase{

    @Test
    public void testApiKeysWorkInPrecedence() throws Exception {
        // Auth only required to register the sys user
        Authentication authMike = setSecurity(mike);
        String apiKey = regEP.registerSystemUser(new RegistrationBean(monowai, mike)).getBody().getApiKey();
        SecurityContextHolder.getContext().setAuthentication(null);
        Assert.assertNotNull(apiKey);
        Fortress fortressA = fortressEP.registerFortress(new FortressInputBean("testApiKeysWorkInPrecedence"), apiKey, null).getBody();
        MetaInputBean inputBean = new MetaInputBean(fortressA.getName(), "wally", "TestTrack", new DateTime(), "ABC123");

        // Fails due to NoAuth or key
        TrackResultBean result;
        try {
            result = trackEP.trackHeader(inputBean, null, null).getBody();
            assertNull(result);
            fail("Security Exception did not occur");
        } catch (SecurityException e) {
            // Good
        }

        // Should work now
        SecurityContextHolder.getContext().setAuthentication(authMike);//

        result = trackEP.trackHeader(inputBean, null, null).getBody(); // Works due to basic authz
        assertNotNull(result);  // works coz basic authz

        final MetaHeader header = trackEP.getMetaHeader(result.getMetaKey(), apiKey, apiKey).getBody();
        assertNotNull(header);
        setSecurity(harry);
        assertNotNull(trackEP.trackHeader(inputBean, apiKey, null));// works
        assertNotNull(trackEP.trackHeader(inputBean, null, apiKey));// works
        assertNotNull(trackEP.trackHeader(inputBean, "invalidApiKey", apiKey));// Header overrides request
        try {
            assertNull(trackEP.trackHeader(inputBean, apiKey, "123")); // Illegal result
            Assert.fail("this should not have worked due to invalid api key");
        } catch (DatagioException e) {
            // this should happen due to invalid api key
        }
        SecurityContextHolder.getContext().setAuthentication(null);// No user context, but valid API key
        assertNotNull(trackEP.trackHeader(inputBean, apiKey, null));// works
        assertNotNull(trackEP.trackHeader(inputBean, null, apiKey));// works
        assertNotNull(trackEP.trackHeader(inputBean, "invalidApiKey", apiKey));// Header overrides request
        try {
            assertNull(trackEP.trackHeader(inputBean, apiKey, "123")); // Illegal result
            Assert.fail("this should not have worked due to invalid api key");
        } catch (DatagioException e) {
            // this should happen due to invalid api key
        }
    }

    @Test
    public void apiCallsSecuredByAccessKey() throws Exception {

        String apiKey = regEP.registerSystemUser(new RegistrationBean(monowai, "123", mike)).getBody().getApiKey();
        // No authorization - only API keys
        SecurityContextHolder.getContext().setAuthentication(null);

        Fortress fortressA = fortressEP.registerFortress(new FortressInputBean("apiCallsSecuredByAccessKey", true), apiKey, null).getBody();
        MetaInputBean inputBean = new MetaInputBean(fortressA.getName(), "wally", "TestTrack", new DateTime(), "ABC9990");

        LogInputBean log = new LogInputBean("harry", new DateTime(),  getRandomMap());
        inputBean.setLog(log);

        TrackResultBean result = trackEP.trackHeader(inputBean, apiKey, apiKey).getBody(); // Works due to basic authz

        assertNotNull(trackEP.getMetaHeader(result.getMetaKey(), apiKey, apiKey).getBody());
        assertNotNull(trackEP.getAuditSummary(result.getMetaKey(), apiKey, apiKey).getBody());
        assertNotNull(trackEP.getTrackTags(result.getMetaKey(), apiKey, apiKey));
        assertNotNull(trackEP.getByCallerRef(result.getFortressName(), result.getDocumentType(), result.getCallerRef(), apiKey, apiKey));
        assertNotNull(trackEP.getByCallerRef(fortressA.getName(), inputBean.getCallerRef(), apiKey, apiKey).iterator().hasNext());
        assertNotNull(trackEP.getLogs(result.getMetaKey(), apiKey, apiKey).iterator().next());
        assertNotNull(trackEP.getLastChange(result.getMetaKey(), apiKey, apiKey));
        Long logId = trackEP.getLogs(result.getMetaKey(), apiKey, apiKey).iterator().next().getId();
        assertNotNull(trackEP.getLogWhat(result.getMetaKey(), logId, apiKey, apiKey));
        assertNotNull(trackEP.getLastChangeWhat(result.getMetaKey(), apiKey, apiKey));
        assertNotNull(trackEP.getFullLog(result.getMetaKey(), logId, apiKey, apiKey));

    }
}