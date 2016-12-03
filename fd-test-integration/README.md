Overview
========

Integration testing is achieved via Docker, DockerCompose and Surefire

We aim to ensure the end-to-end client experience is assured. The stack for integration testing includes
    * fd-engine
    * fd-search
    * elasticsearch
    * rabbitmq
    * fd-client

Before running integration tests you need to have DockerToolbox installed so that your `docker-machine` is running. Don't forget to evaluate your shell environment otherwise you will get an error. Due to some limitations in 3rd party library we used to start the stack, native docker is not available on non-linux platforms.

You can build and verify the entire stack from the `$ flockdata.org` folder

* `mvn verify -P docker,integration` - builds the docker packages and executing unit, functional and integration tests
* `mvn clean install` - builds artifacts running unit and functional tests
* `mvn package -P docker -DskipTests=true` - Just package the Docker containers

If you have built the docker containers you can just execute the integration tests from `fd-test-integration` by running 
`mvn verify -P integration`

While there is no need to manually start the stack to run integration tests you can do so by following these steps

`cd fd-test-integration/src/main/test/resources`

`docker-compose up -d` 

This folder contains a `docker-compose.yml` file that defines how the stack is composed.
