[FlockData](http://FlockData.com) - Headless Content Management for MicroServices
===========

##The Idea
* Your enterprise/systems/services/microservices create and modify organisational data in to their database
  * Have a standarised way of tracking changes to business data for users
  * A Read/write service that simply tracks changes to data in your services
* Headless content management to track state of data 
* Enable users to report on data across data silos

##FlockData
* Aggregating data provides a platform for agile analytics
  * Batch mode or as changes happen
* Information recording api - turning the data in to user centric information for reporting
* Data Lineage - Tracks which service data originates from which system
* Free text searching of your business information - help users find data irrespective of the system that created it
* Keeping audit data out of your transaction processing systems while providing users version history

## Project Status
Stable. Full support for ElasticSearch 2.0 and Neo4j 2.2.x in place. 

## Overview
Collect, Connect Compare and Explore  - FlockData helps you find data you're looking for.

FlockData is a system of insight built as a [DataShore](http://martinfowler.com/bliki/DataLake.html) using the principals of Microservices and Domain Driven Design. We define the service boundary for writes as Audit (tracking data changes) and Search (indexing your data). An exciting off shoot of this idea is that by tracking your data as information you also gain an analytics platform for the powerful query analysis, offered by graph and search databases, indexed in a consistent manner.

Turn your information in to an accessible resource. See if our data [Mission](http://wiki.flockdata.com/pages/viewpage.action?pageId=13172853) aligns with your way of thinking.

##Design
FD follows the design concepts of a [Microservice](http://martinfowler.com/articles/microservices.html). As a service it is in fact itself made up of microservices.

![Alt text](https://bitbucket.org/monowai/flockdata.org/raw/77e20cdf6e83f28cd8db6ee4561a4b0659f06443/micro-service.png?at=develop)

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
Drop us a line over at [flockdata.com](http://flockdata.com/) and we'd be happy to see if we can help you reach your goals. Usual social media channels also apply - [LinkedIn](http://www.linkedin.com/company/3361595) .

## Contributing
We encourage contributions to FlockData from the community. Here’s how to get started.

Latest code is on the develop branch. Master contains last stable release.

* Fork the appropriate sub-projects that are affected by your change. Fork this repository if your changes are for release generation or packaging.
* Create a branch
* Make your changes and run the test suite
* Commit your changes and push them to your fork
* Open pull-requests for the appropriate projects
* We will review your pull-request, suggest changes, and merge it when it’s ready and/or offer feedback

To report a bug or issue, please open a new [issue](https://monowai.atlassian.net/) in our Jira issue tracking system.

### Licensing
We want you and others to get the most out of FD. We use GPL to ensure that changes to FD code can be made shared with everyone. We lay no claim to any of your system or services that are talking to the FD API or databases. 
FlockData is an open source project. We support a Community edition under the GPLv3 license. The Enterprise edition is available under the AGPLv3 license for open source projects otherwise under a commercial license by [contacting](http://flockdata.com/). Talk to us about clustering as there can be costs involved with the underlying stack.
