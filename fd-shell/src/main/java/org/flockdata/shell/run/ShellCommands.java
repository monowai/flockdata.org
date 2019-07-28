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

package org.flockdata.shell.run;

import static org.springframework.shell.standard.ShellOption.NULL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.flockdata.client.Importer;
import org.flockdata.client.commands.CommandResponse;
import org.flockdata.client.commands.EnginePing;
import org.flockdata.client.commands.Health;
import org.flockdata.client.commands.Login;
import org.flockdata.client.commands.RegistrationPost;
import org.flockdata.data.Company;
import org.flockdata.data.SystemUser;
import org.flockdata.helper.JsonUtils;
import org.flockdata.integration.AmqpRabbitConfig;
import org.flockdata.integration.ClientConfiguration;
import org.flockdata.registration.RegistrationBean;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.transform.FdIoInterface;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

/**
 * Runs commands on behalf of the shell
 *
 * @author mholdsworth
 * @tag Command, Shell
 * @since 27/05/2018
 */
@ShellComponent
public class ShellCommands {

  private final LineReader reader;
  //    private Logger logger = LoggerFactory.getLogger(ShellCommands.class);
  private EnginePing enginePing;
  private Health health;
  private ClientConfiguration clientConfiguration;
  private AmqpRabbitConfig rabbitConfig;
  private Login login;
  private RegistrationPost registrationPost;
  private FdIoInterface fdIoInterface;
  private Importer importer;
  private SystemUserResultBean unauthenticated;

  @Autowired
  ShellCommands(FdIoInterface fdIoInterface, ClientConfiguration clientConfiguration, AmqpRabbitConfig rabbitConfig) throws IOException {
    this.fdIoInterface = fdIoInterface;
    this.clientConfiguration = clientConfiguration;
    this.rabbitConfig = rabbitConfig;
    Terminal terminal = TerminalBuilder.terminal();
    reader = LineReaderBuilder.builder()
        .terminal(terminal).build();
    SystemUser su = new SystemUser() {
      @Override
      public String getName() {
        return "unauthenticated";
      }

      @Override
      public String getLogin() {
        return null;
      }

      @Override
      public String getApiKey() {
        return null;
      }

      @Override
      public Company getCompany() {
        return null;
      }

      @Override
      public String getEmail() {
        return null;
      }

      @Override
      public Long getId() {
        return null;
      }

      @Override
      public boolean isActive() {
        return false;
      }
    };
    unauthenticated = new SystemUserResultBean(su);

  }

  /**
   * Commands supported by this ShellRunner
   *
   * @param importer         data file import
   * @param registrationPost register a system user as a data acess user
   * @param login            login under a different system user account
   * @param enginePing       ping the tracking API
   * @param health           verify connectivity health of services
   */
  @Autowired
  void setCommands(Importer importer, RegistrationPost registrationPost, Login login, EnginePing enginePing, Health health) {
    this.enginePing = enginePing;
    this.health = health;
    this.importer = importer;
    this.login = login;
    this.registrationPost = registrationPost;
  }

  //help = "Set shell to talk to an instance of flockdata or display the currently configured instance"
  @ShellMethod(value = "set", key = "set")
  public Collection<String> setApi(
      @ShellOption(help = "The FQDN of FD ") final String url,
      @ShellOption(help = "Rabbit MQ host") final String rabbitHost,
      @ShellOption(help = "Rabbit MQ port default") Integer rabbitPort) {

    if (url != null) {
      clientConfiguration.engineUrl(url);
    }

    if (rabbitHost != null) {
      if (rabbitPort == null) {
        rabbitPort = rabbitConfig.getPort();
      }
      rabbitConfig.resetHost(rabbitHost, rabbitPort);
    }

    return env();
  }

  @ShellMethod(key = {"whoami", "me"}, value = "whoami")
  public SystemUserResultBean whoAmiI() {
    SystemUserResultBean result = fdIoInterface.me();
    if (result == null) {
      return unauthenticated;
    }
    return result;
  }

  //help = "Ping the fd-engine service"
  @ShellMethod(value = "ping")
  public String ping() {
    CommandResponse<String> commandResponse = enginePing.exec();
    return (commandResponse.getError() == null ? commandResponse.getResult() : commandResponse.getError());
  }

  //, help = "Dump the currently defined FD client environment "
  @ShellMethod(value = "env")
  public Collection<String> env() {
    Collection<String> results = new ArrayList<>();
    results.add(clientConfiguration.toString());
    results.add(rabbitConfig.logStatus());
    return results;
  }

  //    , help = "Verify the health of the FD services"
  @ShellMethod(value = "health")
  public String health() {
    CommandResponse response = health.exec();
    return (response.getError() == null ? JsonUtils.pretty(response.getResult()) : response.getError());
  }

  //, help = "register an FD login as a data access user"
  @ShellMethod(value = "register")
  public String register(
      @ShellOption(help = "The login account to register") final String account,
      @ShellOption(help = "The email account to record") final String email,
      @ShellOption(
          help = "If the login manages multiple companies, then assign this login to this one") final String company) {
    RegistrationBean registrationBean = RegistrationBean.builder()
        .companyName((company == null ? clientConfiguration.company() : company))
        .login(account)
        .email(email)
        .build();

    CommandResponse response = registrationPost.exec(registrationBean);
    String result;
    if (response.getError() != null) {
      result = response.getError();
    } else {
      result = JsonUtils.pretty(response.getResult());
    }
    return result;

  }

  //, help = "Login to flockdata"
  @ShellMethod(value = "login")
  public String login(
      @ShellOption(help = "The user name", defaultValue = NULL) String user,
      @ShellOption(help = "The user password", defaultValue = NULL) String pass) {

    if (user == null) {
      user = readLogin(clientConfiguration.httpUser());
    }

    if (pass == null) {
      pass = readPassword();
    }

    if (pass == null) {
      return "No password supplied";
    }

    if (user != null) {
      clientConfiguration
          .httpUser(user)
          .httpPass(pass);
    }
    CommandResponse<SystemUserResultBean> response = login.exec(user, pass);
    clientConfiguration.setSystemUser(response.getResult());
    return (response.getError() == null ? JsonUtils.pretty(response.getResult()) : response.getError());
  }


  //, help = "Track data into the service"
  // ingest --data "data/fd-cow.txt, profile/countries.json;data/states.csv, model/states.json"
  @ShellMethod(value = "ingest")
  public String ingest(
      @ShellOption(help = "<dataFile>,<model>;<dataFile>,<model>;...\r\n" +
          "--data \"datafile.txt,profile.json\" e.g. import \"data/fd-cow.txt, " +
          "profile/countries.json;data/states.csv, model/states.json") final String data) {

    SystemUserResultBean me = whoAmiI();

    if (me == null || me.getApiKey() == null) {
      return "No API key. Have you logged in and is you login a data access account?";
    }

    clientConfiguration.setSystemUser(me);

    Collection<String> filesAndModels;
    if (data == null || data.equals("")) {
      return "No files to parse!";
    }

    filesAndModels = Arrays.asList(data.split(";"));
    return importer.runImport(filesAndModels);
  }

  @ShellMethod(value = "pwd")
  public String pwd() {
    return System.getProperty("user.dir");
  }

  private String readPassword() {
    String question = "password : ";
    return reader.readLine(question, '*');
  }

  private String readLogin(String currentLogin) {

    String question = String.format("login [%s]: ", currentLogin);
    String login = reader.readLine(question);
    if (login.equalsIgnoreCase("")) {
      login = currentLogin;
    }
    return login;
  }


}
