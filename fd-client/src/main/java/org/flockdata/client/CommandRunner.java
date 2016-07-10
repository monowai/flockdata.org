/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.client;

import org.apache.commons.lang3.ArrayUtils;
import org.flockdata.client.commands.Login;
import org.flockdata.client.rest.FdRestWriter;
import org.flockdata.shared.ClientConfiguration;
import org.slf4j.Logger;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Wrapper that determines which functionality to inject to respond to the callers request
 * <p>
 * Created by mike on 27/05/16.
 */
public class CommandRunner {
    public static void main(String[] args) {
        SpringApplication app;
//        System.out.print(ArrayUtils.toString(args));
        if (ArrayUtils.contains(args, "import")) {
            app = new SpringApplication(Importer.class);
            app.setAdditionalProfiles("fd-importer");

        } else if (ArrayUtils.contains(args, "register")) {
            app = new SpringApplication(Register.class);
            app.setAdditionalProfiles("fd-register");
        } else if (ArrayUtils.contains(args, "ping")) {
            app = new SpringApplication(PingRunner.class);
            app.setAdditionalProfiles("fd-ping");

        } else if (ArrayUtils.contains(args, "health")) {
            app = new SpringApplication(HealthRunner.class);
            app.setAdditionalProfiles("fd-health");

        } else {
            System.out.println("");
            System.out.println("Commands supported are import and register");
            System.exit(1);
            return;
        }

        app.setWebEnvironment(false);
        app.setBannerMode(Banner.Mode.OFF);

        // launch the app
        ConfigurableApplicationContext context = app.run(args);

        // finished so close the context
        context.close();
    }

    static void configureAuth(Logger logger, String authUser, ClientConfiguration clientConfiguration, FdRestWriter fdClient) {
        if ( authUser!=null ){

            String[] uArgs = authUser.split(":");
            clientConfiguration.setHttpUser(uArgs[0]);
            clientConfiguration.setHttpPass(uArgs[1]);
            Login login = new Login(clientConfiguration,fdClient);
            login.exec();
            if (login.error()!= null){
                logger.error("Login error for {} - message {}", uArgs[0], login.error());
                System.exit(-1);
            }
            clientConfiguration.setApiKey(login.result().getApiKey());

        }
    }


}