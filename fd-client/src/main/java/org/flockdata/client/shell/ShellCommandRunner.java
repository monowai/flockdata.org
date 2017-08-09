/*
 *  Copyright 2012-2017 the original author or authors.
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

package org.flockdata.client.shell;

import org.flockdata.client.Importer;
import org.flockdata.client.commands.*;
import org.flockdata.helper.JsonUtils;
import org.flockdata.integration.AmqpRabbitConfig;
import org.flockdata.integration.ClientConfiguration;
import org.flockdata.registration.RegistrationBean;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.track.bean.CompanyInputBean;
import org.flockdata.transform.FdIoInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;

/**
 * Runs commands on behalf of the shell
 *
 * @author mholdsworth
 * @tag Command, FdClient, Application, Shell
 * @since 27/05/2016
 */
@Component
public class ShellCommandRunner implements CommandMarker {
    private Logger logger = LoggerFactory.getLogger(ShellCommandRunner.class);
    private EnginePing enginePing;
    private Health health;
    private ClientConfiguration clientConfiguration;
    private AmqpRabbitConfig rabbitConfig;
    private Login login;
    private RegistrationPost registrationPost;
    private FdIoInterface fdIoInterface;
    private Importer importer;

    @Autowired
    ShellCommandRunner(FdIoInterface fdIoInterface, ClientConfiguration clientConfiguration, AmqpRabbitConfig rabbitConfig) {
        this.fdIoInterface = fdIoInterface;
        this.clientConfiguration = clientConfiguration;
        this.rabbitConfig =rabbitConfig;
    }

    /**
     * Commands supported by this ShellRunner
     * @param importer          data file import
     * @param registrationPost  register a system user as a data acess user
     * @param login             login under a different system user account
     * @param enginePing        ping the tracking API
     * @param health            verify connectivity health of services
     */
    @Autowired
    void setCommands(Importer importer, RegistrationPost registrationPost, Login login, EnginePing enginePing, Health health) {
        this.enginePing = enginePing;
        this.health = health;
        this.importer = importer;
        this.login = login;
        this.registrationPost = registrationPost;
    }

    @CliCommand(value = "set", help = "Set shell to talk to an instance of flockdata or display the currently configured instance")
    public String setApi(
            @CliOption(key = {"api"}, help = "The FQDN of FD ") final String url,
            @CliOption(key = {"rabbit.host"}, help = "Rabbit MQ host") final String rabbitHost,
            @CliOption(key = {"rabbit.port"}, help = "Rabbit MQ port default") Integer rabbitPort) {

        if (url != null) {
            clientConfiguration.setServiceUrl(url);
        }

        if (rabbitHost != null) {
            if (rabbitPort == null)
                rabbitPort = rabbitConfig.getPort();
            rabbitConfig.resetHost(rabbitHost, rabbitPort);
        }

        return config();
    }

    @CliCommand(value = "ping", help = "Ping the fd-engine service")
    public String ping() {
        CommandResponse<String> commandResponse = enginePing.exec(fdIoInterface);
        return (commandResponse.getError() == null ? commandResponse.getResult() : commandResponse.getError());
    }

    @CliCommand(value = "env", help = "Dump the currently defined FD client environment ")
    public String config() {
        String result = clientConfiguration.toString();
        return result + "\r\n" + rabbitConfig.logStatus();
    }

    @CliCommand(value = "health", help = "Verify the health of the FD services")
    public String health() {
        CommandResponse response = health.exec(fdIoInterface);
        return (response.getError() == null ? JsonUtils.pretty(response.getResult()) : response.getError());
    }

    @CliCommand(value = "register", help = "register an FD login as a data access user")
    public String register(
            @CliOption(key = {"login"}, help = "The login account to register") final String account,
            @CliOption(key = {"email"}, help = "The email account to record") final String email,
            @CliOption(key = {"company"}, help = "If the login manages multiple companies, then assign this login to this one") final String company) {
        RegistrationBean registrationBean = new RegistrationBean(clientConfiguration.getCompany(), account)
                .setEmail(email);

        if (company != null)
            registrationBean.setCompany(new CompanyInputBean(company));

        CommandResponse response = registrationPost.exec(fdIoInterface, registrationBean);
        String result;
        if (response.getError() != null) {
            result = response.getError();
        } else {
            result = JsonUtils.pretty(response.getResult());
        }
        return result;

    }

    @CliCommand(value = "login", help = "Login to flockdata")
    public String login(
            @CliOption(key = {"user"}, help = "The user name") String user,
            @CliOption(key = {"pass"}, help = "The user password") String pass) {

        if (user==null )
            user =login.readLogin( clientConfiguration.getHttpUser());

        if ( pass == null )
            pass = login.readPassword();

        if ( pass ==null )
            return "No password supplied";

        if (user != null) {
            clientConfiguration.setHttpUser(user);
            clientConfiguration.setHttpPass(pass);
        }
        CommandResponse<SystemUserResultBean> response = login.exec(fdIoInterface, user, pass);
        clientConfiguration.setSystemUser(response.getResult());
        return (response.getError() == null ? JsonUtils.pretty(response.getResult()) : response.getError());
    }


    @CliCommand(value = "import", help = "Track data into the service")
    public String importData(
            @CliOption(key = {"data"}, mandatory = true, help = "--data \"datafile.txt,profile.json\" e.g. import \"data/fd-cow.txt, profile/countries.json;data/states.csv, model/states.json")
            final String fileInput) {

        if (clientConfiguration.getApiKey() == null) {
            return "No API key. Have you logged in and is you login a data access account?";
        }
        Collection<String> filesAndModels;
        if (fileInput == null || fileInput.equals("")) {
            return "No files to parse!";
        }

        filesAndModels = Arrays.asList(fileInput.split(";"));
        return importer.runImport(filesAndModels);
    }

}
