Overview
========
fd-client is an Apache licensed client side library used to talk to FlockData. It contains classes that can be injected into your Java code and commands that can be executed from the command line
    
Configuration is via YAML and can be overridden by passing arguments on the command line.

### Client Utilities

We recommend using the Docker package `flockdata/fd-client`. The commands in this section assume you are running the [fd-demo](http://github.com/monowai/fd-demo) stack

## fdregister
FlockData allows you to allow users in your authentication domain to access data. This is happens when you connect a login account with FlockData as a "System User" account. System Users have access to data curated in FlockData. Docker-Compose defines where the `fd-engine` api is located where running with `docker` means you have to tell it where to find `fd-engine` 

To register a data access account, in the fd-demo stack, run the following:
`docker-compose run fd-client fdregister -u=demo:123 -l=demo`

If you are just running Docker, then you will need to tell it how to find fd-engine:
`docker run fd-client fdregister -u=demo:123 -l=demo --org.fd.engine.url=http://fd-engine:8080`

These commands do the same thing - register the login account `demo` as a SystemUser so that it can read and write data

## fdimport
Invoke the fdimporter to ingest data as modeled by `profile.json` using supplied arguments
 
`docker-compose run fdimport -u=demo:123 --fd.client.import="path/data.txt, path/profile.json`

## fdcountries
See the world! This customized version of `fdimport` loads [countries](http://opengeocode.org/), capital cities, and common aliases along with geo tagged data, into FlockData. States for USA, Canada, and Australia are also added.

`docker-compose run fd-client fdcountries -u=demo:123`

### Embedding Client Classes
FlockData has a comprehensive REST based API. This package provies convenience classes to facilitate client side communication. Classes in this package support injection using the SpringFramework. Key classes include

* `ClientConfiguration` - access and set various components properties
* `FdBatchWriter` - Client template to talk to the service over REST and AMQP

### Commands
Commands are being added on a regular basis and provide a functional wrapper to the REST services. These classes, and how they are used, can be found in the package `org.flockdata.client.commands`

* Ping         - Can you see the service
* Login        - Authenticate with the service
* Health       - Validate health checks
* TrackEntity  - Write an entity to the service
* Registration - Register an authorised login as a data-access user 
* EntityGet    - Retrieve a tracked entity from the service.

ToDo: Document the use
`register --auth.user=demo:123 --register.login=mike --org.fdengine.api=http://docker-ip:8080`

### Configuration
All test based configuration is controlled by files in `src/test/resources/`

* `application.yml`     - General client configuration settings for client side classes
* `application_dev.yml` - Unit test client configuration settings
* `fd-batch.properties` - Spring batch client configuration settings

Configured settings can always be overridden on the command line or set as system environment variables