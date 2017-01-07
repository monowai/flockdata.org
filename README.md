[FlockData](http://FlockData.com) - Data management framework
===========

FlockData lets you visually model your system data and connect it to core business domain concepts in your organisation.

Designed as a collection of modularized services, FD tracks and integrates changes to your data happening in core business systems. You can integrate data across systems and enrich it with data from other sources in a controlled manner. 
FD takes a domain driven approach to data normalization which enables users of all levels to engage with your data with minimal friction as data is modeled with the organisation context in mind - Invoice, Customer, Policy etc. The underlying structure is largely irrelevant at this level of abstraction. Full access to the underlying data, using a unified access pattern, allows you to build beautifully rich cross application data solutions. 

FD provides data transformation, information integration and reporting for any data.  Using the power of NoSQL, data is written to Neo4j and ElasticSearch where it can be efficiently and effectively queried in dynamic ways to uncover insights in your data with minimal effort.

 * Model management standardizes and reduces the effort in integration by modelling conceptual organisational structures from your existing data
 * Information is integrated between Neo4j - for connected analysis - and ElasticSearch for aggregate analysis.
 * Reporting structures are captured to provide dynamic reporting capabilities with little programmer intervention

FD can take any data, including your SQL data/queries/views, and pipes it into graph and content database for sophisticated analysis and secured access.

    Model->Load->Analyze

## Overview

FlockData allows you to model your data and normalize it around the business context - the conceptual model. The results allow you to 

  * Standardised way in which data can be securely read and analyzed
  * Independence from the underlying structure of your data 
  * Track the conceptual structure of your data to understand how it is used across system 

All functionality, including querying of the databases, can be performed over REST APIs or using any JSON aware reporting tool.

## How
Model the data you want to import, load it then start analyzing using your favourite tools

FlockData is a system of insight built as a [DataShore](http://martinfowler.com/bliki/DataLake.html) using the principals of Microservices and Domain Driven Design. By tracking your data using DDD your data is indexed in a consistent manner and offers enhanced analytics capabilities using graph and search databases, 

This approach to data modelling makes it intuitive to model and integrate data. Standardizing the language used to describe data improves conversations and can improve engagement across groups of varying technical levels.

## Design
FlockData is inspired by a combination of Domain Driven Design and [Microservice](http://martinfowler.com/articles/microservices.html) goals.

    Entities represent the high level "things" your organization should care about
    Tags represent "concepts" - that help you index the entities for retrieval

Loosely coupled services are the way to build systems to support independent scaling and reduce impact of change. 

## Technical
FD is built on Spring Boot, Integration, Security, MVC etc, RabbitMQ, Neo4j, ElasticSearch, and Riak. [Docker images](https://hub.docker.com/u/flockdata/) are available for each service

We have crafted integration between these services to be as configurable and easy to deploy as possible. To this end we really do suggest you start with the docker-compose functionality to get going with the minimum of fuss and to more easily understand the extensive configuration and concepts involved with this stack. 

This stack requires Java 8 and mvn 3.x if you're going to build it. If you want to build docker images then you will also require Docker to be installed.

The docker-compose script includes [fd-view](http://github.com/monowai/fd-view), our AngularJS based data modelling, administration and reporting framework. Use to model data files and give you an idea of the functionality you can add to your applications.

We use the mvn spotify project for docker management and run integration tests using testcontainers in Docker -see [fd-docker-integration](fd-docker-integration/README.md)

fd-client allows you to inject the necessary client components directly into your application giving you the power of search and graph in next to no time. It also offers support for unit testing your interactions with the service.

## Docker
We love Docker. We even have a docker-compose script to make evaluation a snap. 
 
     `git clone http://github.com/monowai/fd-demo`
     `docker-compose up -d`
     `http://docker-ip - login as demo/123`
       
Further details can be found on [fd-demo](http://github.com/monowai/fd-demo)

## Datastores
FlockData coordinates and directs information to various highly scalable data sources to achieve functionality. 
 * [elasticsearch](https://github.com/elasticsearch/elasticsearch)
 * [neo4j](https://github.com/neo4j/neo4j)
 * [redis](https://github.com/antirez/redis)
 * [riak](http://basho.com/riak/)
 * [rabbitmq](https://github.com/rabbitmq/rabbitmq-server)

## Postman
Poke around the API using the supplied postman [Postman](https://github.com/monowai/flockdata.org/blob/master/fd.api-postman.json) package. This contains environments for both localhost DEV and docker and gives you an idea of the rich set of features that can be invoked 

## Features

### Explore
Neo4J is the world leading graph database enabling you to explore how your business information is connected. FD builds a graph for you by using information tags that you determine to be of value. Start exploring your enterprise information as a social graph in hours.

### Search
Build efficiently structured ElasticSearch databases, ready to query, directly from SQL queries. Sending FD your application data you can offer your organzation Full Text Search capabilities across business data that your users will understand. 
  
Using [Kibana](http://www.elasticsearch.org/overview/kibana/) on the front and you can start to accumulate realtime dashboards that give you the pulse of change in your business. 

Think LogStash for application data. FlockData maintains your ElasticSearch data giving you cross-system search capabilities in a matter of hours.

### Compare
FlockData can record changes to your data giving you an event source that lets everyone recall the state of domain documents at any point in time.

## Get going
Take some time to review FlockData's key services
 * [fd-client](fd-client/README.md) - apache licenced library that allow read/write data access to the services from the commandline or within your own code
 * [fd-engine](fd-engine/README.md) - tracks your entities and tags as metadata into Neo4j
 * [fd-search](fd-search/README.md) - search writer that builds ElasticSearch databases from graph data stored in fd-engine 
 * [fd-store](fd-store/README.md) - blob store for data

Contact us if you'd like access to some of the analysis repos we have put together. Not all of these are currently public on GitHub

## Work with us
This is a platform to create well managed data applications. We want to help you and welcome contributions. Drop us a line over at [flockdata.com](http://flockdata.com/) and we'd be happy to have a ramblechat to see if we can help you reach your goals.

## Contributing
We encourage contributions to FlockData from the community. Here’s how to get started.

 * Fork the project
 * Create a branch
 * Make your changes and run the functional and integration test suite `mvn verify -P docker,integration`
 * Commit your changes and push them to your fork
    * Please interactively rebase your commits to aid review
 * Open pull-requests for you changes
 * We will review your pull-request, suggest changes, and merge it when it’s ready and/or offer feedback

To report a bug or issue, please register and open a new [issue](https://monowai.atlassian.net/) in our Jira issue tracking system.

### Licensing
We use a mix of Apache and GPL licenses. 

fd-client is the Apache licensed library providing access to the GPL licensed backend of fd-engine, fd-store and fd-search.
We know there is some resistance to GPL, so why do we use it? It's to ensure that changes to core FD services, the stuff that runs behind the API, can be made shared with everyone including us. We want everyone to get the most out of data using FD. 
Our licenses place no restrictions whatsoever on your data or applications. In fact you could build your databases then remove FD from the picture completely.

Talk to us about clustering/backup as there may be 3rd party licensing costs involved with the underlying databases. We can offer commercial licenses if required by [contacting](http://flockdata.com/)
 
 
