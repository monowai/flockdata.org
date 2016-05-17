Overview
========
fd-client contains classes that deal with integrating your code to the FD data processing stack. All configuration is via YAML or can be overridden by passing arguments on the command line.

We also perform integration testing using the classes in this package.

### Integration Testing
We validate the end-to-end client experience with integration tests. To run integration tests you will require Docker 1.11.x and Docker Compose 1.7x already installed and that your `docker-machine` is running. Don't forget to evaluate your shell environment otherwise you will get an error.

The following example commands assume you are in the `$ flockdata.org` folder

* `mvn clean install` - builds artifacts running unit and functional tests
* `mvn package -P docker -DskipTests=true` - as above, but additionally builds the Docker containers, used by integration testing, and skips any functional tests
* `mvn verify -P docker,integration` - builds the docker packages and executing unit, functional and integration tests

If you have already built the docker containers you can execute the integration tests from the `fd-client` folder with `mvn verify -P integration`

You can manually start integration stack from the folder `fd-client/src/main/test/resources/int` by running the command `docker-compose up -d`. This folder contains the `docker-compose.yml` file that defines how the stack is composed.

There is no need to manually start the stack to run integration tests as this is done for you automatically.

### Client Side Classes
FlockData has a comprehensive REST based API. This package provies convenience classes to facilitate client side communication. Classes in this package support injection using the SpringFramework. Key classes include

* `ClientConfiguration` - settings necessary to configure various components
* `FdRestWriter` - HTTP client to talk to the service over HTTP
* `AmqpServices` - AMQP services to talk with RabbitMQ

### Commands
Commands are being added on a regular basis but essentially provide a REST based wrapper to the service. These classes, and how they are used, can be found in the package `org.flockdata.client.commands`

* Ping        - Can you see the service
* Login       - Authenticate with the service
* Health      - Validate health checks
* TrackEntity - write an entity to the service
* EntityGet   - retrieve a tracked entity from the service.

Please refer to `src/test/java/org/flockdata/test/integration` for examples.

### Configuration
All test based configuration is controlled by files in `src/test/resources/`

* `application-int.yml` - Integration client configuration settings that configure the client side classes
* `application_dev.yml` - Unit test client configuration settings
* `fd-batch.properties` - Spring batch client configuration settings

Configured settings can be overridden on the command line.
