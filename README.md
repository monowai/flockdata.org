[FlockData](http://FlockData.com) - Data platform and information integration service
===========

## Use Cases
* System of Insight - Power statistical based analysis such as recommendations, fraud analytics, sales patterns etc.
* Data Lineage - what data is changing and by whom across your systems
* System migration - move data from legacy systems and keep it available for insight purposes.
* Information Integration - Connect your data across systems to find patterns
* Free text searching of your business information
* Keeping the audit data out of your transaction processing systems and provide users with full history tracking of their data
* Recommendation engine - With your information connected, you can ask "people who brought this also brought...." questions
* [Event Sourcing](http://martinfowler.com/eaaDev/EventSourcing.html) in complex integrated environments.

## Project Status
Full support for ElasticSearch 1.4x and Neo4j 2.1.x in place.
Significant enhancements to the way FlockData profiles information to create insight.
Resilience and reliability have been the watchwords of the day. RIAK and RabbitMQ are now supported by default.
Working on write performance.

## Overview
Collect, Connect Compare and Explore  - FlockData helps you find what you're looking for.

FlockData is a system of insight built as a [DataShore](http://martinfowler.com/bliki/DataLake.html) using the principals of Microservices and Domain Driven Design. The aim is to provide an authoritative data source to power systems of engagement that answer knowledge based questions from your data.

Which customers influence other customer purchases? Which customers who buy beer, also by games? Where did we ship these batch numbers to? Alert me to all trades > $1,000,000 involving Trader Joe on the Sell side? Complex questions don't become complex projects. They become realtime dashboards, alerts and queries that can be produced in a morning.

FD known you want to aggregate data from your existing system and look at it new ways to uncover insights you didn't know existed. This real-time anaysis saves time and enables you to identify signal from noise with free-form exploration. This is hugely useful when you are not quite sure what you are looking for in your information and are forming hypotheses on the journey to insight.

All your data is stored as industry as standard JSON documents in open-source database accessible to any application that can talk to a website. This lets you take advantage of next generation visulisation tools and existing number crunching tools like Excel.

Turn your information in to an accessible resource. See if our data [Mission](http://www.monowai.com/wiki/pages/viewpage.action?pageId=13172853) aligns with your way of thinking.

##Architecture
FD follows the design concepts of a [Microservice](http://martinfowler.com/articles/microservices.html). As a service it is in fact itself made up of microservices.

![Alt text](https://bitbucket.org/monowai/flockdata.org/raw/77e20cdf6e83f28cd8db6ee4561a4b0659f06443/micro-service.png?at=develop)

FlockData is inspired by Domain Driven Design, ROA & SOA architectures. It integrates sophisticated index technologies and document management logic in to highly scalable JSON databases. The result lets you look at your information in new and innovative ways to discover patterns in information.

Loosely coupled services have become the way to build systems to support independent scaling and reduce impact of change. Tracking the information that flows across these services becomes vital when it comes to analysing issues both technically and from the user's experience. FD supports this approach by using [Event Sourcing](http://martinfowler.com/eaaDev/EventSourcing.html) techniques.

##Datastores
FlockData coordinates and directs information to various highly scalable data sources to achieve its benefits. These teams are genius. 
* [elasticsearch](https://github.com/elasticsearch/elasticsearch)
* [neo4j](https://github.com/neo4j/neo4j)
* [resdis](https://github.com/antirez/redis)
* [riak](http://basho.com/riak/)
* [rabbitmq](https://github.com/rabbitmq/rabbitmq-server)

## Features
### Search
[ElasticSearch](htt://www.elastic.co) is a specialized database enabling you to create complex keyword queries and sophisticated aggregations that respond quickly.  Put [Kibana](http://www.elasticsearch.org/overview/kibana/) on the front and you can start to accumulate realtime dashboards that give you the pulse of change in your business. With FD tapping in to your applications, you can offer your staff Full Text Search capabilities across your actual business data - FD makes it possible to search "the latest version" of your business document, i.e. your Customer record, Invoice, Inventory item, whatever; irrespective of what underlying system created it. 

FlockData maintains your ElasticSearch data giving you cross-system search capabilities in hours.

### Explore
Neo4J is a graph database that let's you explore your business information in a highly connected graph. FD will build this graph for you by using information tags that you determine to be of value. You can start exploring your enterprise information as a social graph in hours.

## How to get going
You need to gain a brief understanding of two of the key FlockData scalable micro services

* [fd-engine](fd-engine/README.md) - tracks changes to your data - think LogStash for business data
* [fd-search](fd-search/README.md) - find entity information tracked by fd-engine into ElasticSearch

Familiarise yourself with the general API on our [Wiki](http://www.monowai.com/wiki/pages/viewpage.action?pageId=13172790)

## Working with us
Drop us a line over at [flockdata.com](http://flockdata.com/) and we'd be happy to see if we can help you reach your goals. Usual social media channels also apply - [LinkedIn](http://www.linkedin.com/company/3361595) .

## Contributing
We encourage contributions to FlockData from the community. Here’s how to get started.

Latest code is on the develop branch

* Fork the appropriate sub-projects that are affected by your change. Fork this repository if your changes are for release generation or packaging.
* Create a branch
* Make your changes and run the test suite.
* Commit your changes and push them to your fork.
* Open pull-requests for the appropriate projects.
* We will review your pull-request, suggest changes, and merge it when it’s ready and/or offer feedback.

To report a bug or issue, please open a new [issue](https://monowai.atlassian.net/) in our Jira issue tracking system.

### Licensing
FlockData is an open source project. We support a Community edition under the GPLv3 license. The Enterprise edition is available under the AGPLv3 license for open source projects otherwise under a commercial license by [contacting](http://flockdata.com/). Talk to us about clustering as there can be costs involved with the underlying stack.
