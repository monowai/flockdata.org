[FlockData](http://FlockData.com) - Information Integration Services
===========

FlockData mediates data flows from any datastore in to Neo4j and ElasticSearch. It is an information processing pipeline.

FD utilises Spring Boot, Integration, Security, MVC etc, RabbitMQ, Neo4j, ElasticSearch, and Riak. Deployment is by configuration and each module has a Docker image built and made availiable.

Using FD reduces the effort in keeping pace with change across a stack such as this.

This stack requires Java 8 and if you're going to build it, then also mvn 3.x. If you want to build docker images then you will also require Docker to be installed.

We use the mvn spotify project for docker management, and run integration tests using testcontainers in Docker. This can all be found in the fd-client project

##Docker
Stack is now entirely executed through Docker with a compose script. Please see [fd-demo](http://github.com/monowai/fd-demo) for more details on how to start the stack in various ways .

![Alt text](https://github.com/monowai/flockdata.org/blob/master/docker.png)

## Postman
The API can be invoked via the supplied postman [Postman](https://github.com/monowai/flockdata.org/blob/master/fd.api-postman.json). This contains environments for both localhost DEV and docker

## What
* FD combines ideas from Master Data Management, Content Management and Datawarehousing without actually being any of those things
* FD guides you to look at data as information that need to be analysed which is achieved through acombination of
  * Heterogeneous storage capabilities
  * Standardised path in which to read and write data securely
  * Being agnostic to the structure of your data
* FD tracks metadata and builds the structures in to NoSQL databases ready for you to analyse

## Overview
Collect, Connect Compare and Explore  - FlockData helps you find data you're looking for.

FlockData is a system of insight built as a [DataShore](http://martinfowler.com/bliki/DataLake.html) using the principals of Microservices and Domain Driven Design. We define the service boundary for writes as Audit (tracking data changes) and Search (indexing your data). An exciting off shoot of this idea is that by tracking your data as information you also gain an analytics platform for the powerful query analysis, offered by graph and search databases, indexed in a consistent manner.

Turn your information in to an accessible resource. See if our data [Mission](http://wiki.flockdata.com/pages/viewpage.action?pageId=13172853) aligns with your way of thinking.

##Design
FD follows the design concepts of a [Microservice](http://martinfowler.com/articles/microservices.html). As a service it is in fact itself made up of microservices.

![Alt text](https://github.com/monowai/flockdata.org/blob/master/micro-service.png)

FlockData is inspired by Domain Driven Design (the way we look at information), ROA & SOA architectures (API access). It integrates sophisticated index technologies and document management logic in to highly scalable JSON databases. The result lets you look at your information in new and innovative ways to discover patterns in information.

Loosely coupled services have become the way to build systems to support independent scaling and reduce impact of change. Tracking the information that flows across these services becomes vital when it comes to analysing issues both technically and from the user's experience. FD supports this approach by using [Event Sourcing](http://martinfowler.com/eaaDev/EventSourcing.html) techniques.

##Datastores
FlockData coordinates and directs information to various highly scalable data sources to achieve functionality. 
* [elasticsearch](https://github.com/elasticsearch/elasticsearch)
* [neo4j](https://github.com/neo4j/neo4j)
* [redis](https://github.com/antirez/redis)
* [riak](http://basho.com/riak/)
* [rabbitmq](https://github.com/rabbitmq/rabbitmq-server)

##Features
### Search

[ElasticSearch](htt://www.elastic.co) is a specialized database enabling you to create complex keyword queries and sophisticated aggregations that respond quickly.  Put [Kibana](http://www.elasticsearch.org/overview/kibana/) on the front and you can start to accumulate realtime dashboards that give you the pulse of change in your business. With FD tapping in to your applications, you can offer your staff Full Text Search capabilities across your actual business data - FD makes it possible to search "the latest version" of your business document, i.e. your Customer record, Invoice, Inventory item, whatever; irrespective of data underlying system created it. 

Think LogStash for application data. FlockData maintains your ElasticSearch data giving you cross-system search capabilities in hours.

### Explore
Neo4J is the world leading graph database enabling you to explore how your business information is connected. FD builds a graph for you by using information tags that you determine to be of value. Start exploring your enterprise information as a social graph in hours.

## How to get going
You need to gain a brief understanding of two of the key FlockData scalable micro services

* [fd-engine](fd-engine/README.md) - tracks changes to your data - think LogStash for business data
* [fd-search](fd-search/README.md) - find entity information tracked by fd-engine into ElasticSearch

Familiarise yourself with the general API on our [Wiki](http://wiki.flockdata.com/pages/viewpage.action?pageId=13172790)

## Working with us
Drop us a line over at [flockdata.com](http://flockdata.com/) and we'd be happy to have a ramblechat to see if we can help you reach your goals. Usual social media channels also apply - [LinkedIn](http://www.linkedin.com/company/3361595) .

## Contributing
We encourage contributions to FlockData from the community. Here’s how to get started.

* Fork the project
* Create a branch
* Make your changes and run the test suite `mvn verify`
* Commit your changes and push them to your fork
    * Please interactively rebase your commits to aid review
* Open pull-requests for you changes
* We will review your pull-request, suggest changes, and merge it when it’s ready and/or offer feedback

To report a bug or issue, please register and open a new [issue](https://monowai.atlassian.net/) in our Jira issue tracking system.

### Licensing
We want everyone to get the most out of FD. We use GPL to ensure that changes to FD code, the stuff that runs behind the API, can be made shared with everyone.
FlockData is an open source project. We support a Community edition under the GPLv3 license. The Enterprise edition is available under the AGPLv3 license for open source projects otherwise under a commercial license by [contacting](http://flockdata.com/). Talk to us about clustering as there can be external costs involved with the underlying stack.
