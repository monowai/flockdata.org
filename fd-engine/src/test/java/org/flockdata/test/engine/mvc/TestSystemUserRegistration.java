/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.engine.mvc;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.flockdata.helper.JsonUtils;
import org.flockdata.registration.RegistrationBean;
import org.flockdata.registration.SystemUserResultBean;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * @author mholdsworth
 * @tag Test, SystemUser, MVC
 * @since 28/08/2014
 */
public class TestSystemUserRegistration extends MvcBase {

    @Test
    public void testUnauthorisedToCreateUser() throws Exception {
        setSecurityEmpty();
        // Unauthenticated users can't register accounts
        exception.expect(AccessDeniedException.class);
        makeDataAccessProfile(noUser(), ANYCO, "a-user", MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    public void registrationFlow() throws Exception {

        // default - admin user
        setSecurity();

        // Retry the operation
        SystemUserResultBean regResult
            = registerSystemUser(mike(),
            RegistrationBean.builder().companyName(ANYCO)
                .login("new-user")
                .email("anyone@anywhere.com")
                .build());
        assertNotNull(regResult);
        assertEquals("new-user", regResult.getLogin());
        assertNotNull(regResult.getCompanyName());
        assertEquals("anyone@anywhere.com", regResult.getEmail());
        assertNotNull(regResult.getApiKey());
        setSecurityEmpty();

        // Check we get back a Guest
        regResult = getMe(noUser());
        assertNotNull(regResult);
        assertEquals("noone", regResult.getLogin().toLowerCase());


        regResult = getMe(harry());
        assertNotNull(regResult);
        assertEquals(harry, regResult.getLogin());
        assertNotNull(regResult.getApiKey());

        // Assert that harry, who is not an admin, cannot create another user
        exception.expect(AccessDeniedException.class);
        makeDataAccessProfile(harry(), regResult.getCompanyName(), regResult.getCompanyName(), MockMvcResultMatchers.status().isForbidden());


    }

    @Test
    public void concurrentUserRegistration() throws Exception {
        // Tracking down what appears to be a concurrency issue on CircleCI

        int runnerCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);

        int count = 0;
        Collection<DataAccessRunner> runners = new ArrayList<>();
        ExecutorService executor = Executors.newCachedThreadPool();

        while (count < runnerCount) {
            DataAccessRunner runner = new DataAccessRunner(count, startLatch);
            executor.execute(runner);
            runners.add(runner);
            count++;
        }
        startLatch.countDown(); // Start the runners
        Collection<Integer> completed = new ArrayList<>();

        int finishCount = 0;
        while (finishCount != runners.size()) {
            for (DataAccessRunner runner : runners) {

                if (!completed.contains(runner.id) && runner.finished) {
                    completed.add(runner.id);
                    finishCount++;
                    assertTrue(runner.worked);
                }
            }
        }

    }

    SystemUserResultBean registerSystemUser(RequestPostProcessor user, RegistrationBean register) throws Exception {

        MvcResult response = mvc().perform(MockMvcRequestBuilders.post(MvcBase.apiPath + "/profiles/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.toJson(register)
            )
            .with(user)
        ).andExpect(MockMvcResultMatchers.status().isCreated()).andReturn();

        return JsonUtils.toObject(response.getResponse().getContentAsByteArray(), SystemUserResultBean.class);
    }

    SystemUserResultBean getMe(RequestPostProcessor user) throws Exception {

        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(MvcBase.apiPath + "/profiles/me/")
            .contentType(MediaType.APPLICATION_JSON)
            .with(user)

        ).andReturn();

        return JsonUtils.toObject(response.getResponse().getContentAsByteArray(), SystemUserResultBean.class);
    }

    class DataAccessRunner implements Runnable {
        boolean worked = true;
        int runCount = 10;
        CountDownLatch startLatch;
        boolean finished;
        int id;

        DataAccessRunner(int id, CountDownLatch startLatch) {
            this.startLatch = startLatch;
            this.id = id;
        }

        @Override
        public void run() {

            try {
                startLatch.await();
                logger.info("{} is running", id);
                int i = 0;
                while (i < runCount) {
                    setSecurity();
                    SystemUserResultBean regResult = registerSystemUser(mike(), RegistrationBean.builder()
                        .companyName(ANYCO)
                        .login(harry)
                        .build());
                    assertNotNull(regResult);
                    i++;
                }

            } catch (Exception e) {
                logger.error(e.getMessage());
                worked = false;
            } finally {
                startLatch.countDown();
            }
            finished = true;
        }
    }

}
