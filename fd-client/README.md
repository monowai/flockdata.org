Overview
========
fd-client contains functionality that deals with:
    * Running functions from the command line
    * integrating your code to the FD data processing stack.
    
Configuration is via YAML and can be overridden by passing arguments on the command line.

### Client Utilities

We recommend using the Docker package `flockdata/fd-client`. The commands in this section assume you are running the [fd-demo](http://github.com/monowai/fd-demo) stack

## fdregister
FlockData allows you to allow users in your authentication domain to access data. This is happens when you connect a login account with FlockData as a "System User" account. System Users have access to data curated in FlockData. Docker-Compose defines where the `fd-engine` api is located where running with `docker` means you have to tell it where to find `fd-engine` 

To register a data acess account, in the fd-demo stack, run the following:
`docker-compose run fdregister -u=demo:123 -l=demo`

If you are just running Docker, then you will need to tell it how to find fd-engine:
`docker run fdregister -u=demo:123 -l=demo --org.fd.engine.url=http://fd-engine:8080`

These commands do the same thing - register the login account `demo` as a SystemUser so that it can read and write data

## fdimport
Invoke the fdimporter to ingest data shaped by the `profile.json` supplied in the arguments
 
`docker-compose run fdimport -u=demo:123 --fd.client.import="path/data.txt, path/profile.json`

## fdcountries
See the world! This customized version of `fdimport` loads [countries](http://opengeocode.org/), capital cities, and common aliases along with geo tagged data, into FlockData. States for USA, Canada, and Australia are also added.

`docker-compose run fdcountries -u=demo:123`

### Integration Testing

Integration testing run using classes in this package.

The end-to-end client experience is validated with integration tests. To run integration tests you will require Docker 1.11.x and Docker Compose 1.7x already installed and that your `docker-machine` is running. Don't forget to evaluate your shell environment otherwise you will get an error.

The following example commands assume you are in the `$ flockdata.org` folder

* `mvn clean install` - builds artifacts running unit and functional tests
* `mvn package -P docker -DskipTests=true` - as above, but additionally builds the Docker containers, used by integration testing, and skips any functional tests
* `mvn verify -P docker,integration` - builds the docker packages and executing unit, functional and integration tests

If you have already built the docker containers you can execute the integration tests from the `fd-client` folder with `mvn verify -P integration`

You can manually start integration stack from the folder `fd-client/src/main/test/resources/int` by running the command `docker-compose up -d`. This folder contains the `docker-compose.yml` file that defines how the stack is composed.

There is no need to manually start the stack to run integration tests as this is done for you automatically.

### Embedding Client Classes
FlockData has a comprehensive REST based API. This package provies convenience classes to facilitate client side communication. Classes in this package support injection using the SpringFramework. Key classes include

* `ClientConfiguration` - settings necessary to configure various components
* `FdRestWriter` - HTTP client to talk to the service over HTTP
* `AmqpServices` - AMQP services to talk with RabbitMQ

### Commands
Commands are being added on a regular basis but essentially provide a REST based wrapper to the service. These classes, and how they are used, can be found in the package `org.flockdata.client.commands`

* Ping         - Can you see the service
* Login        - Authenticate with the service
* Health       - Validate health checks
* TrackEntity  - Write an entity to the service
* Registration - Register an authorised login as a data-access user 
* EntityGet    - Retrieve a tracked entity from the service.

Please refer to `src/test/java/org/flockdata/test/integration` for examples.

### Configuration
All test based configuration is controlled by files in `src/test/resources/`

* `application.yml`     - General client configuration settings for client side classes
* `application-int.yml` - Integration client configuration settings for client side classes
* `application_dev.yml` - Unit test client configuration settings
* `fd-batch.properties` - Spring batch client configuration settings

Configured settings can always be overridden on the command line or as `ENV` settings