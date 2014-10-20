[FlockData](http://FlockData.com) - Data Framework and Mediation Service
===========
## Project Status
Goodbye AuditBucket and hello FlockData. We are changing the name. But this is not yet completed. Most anything referring to ab- will become fd-
Full support for ElasticSearch 1.3 and Neo4j 2.1.x in place. API is largely settled. 
fd-security has been introduced as a pluggable authentication service. We support Stormpath and simple user names/passwords in XML

Attachment support has now been introduced. Binary attachments can be loaded and searched for.

## How to get going
You need to gain a brief understanding of two of the key FlockData scalable micro services
* [fd-engine](ab-engine/README.md) - track changes to your Entity information - think LogStash for business data  
* [fd-search](ab-search/README.md) - find entity information tracked by fd-search into ElasticSearch

Familiarise yourself with the general API on our [Wiki](http://www.monowai.com/wiki/pages/viewpage.action?pageId=13172790)

## Overview
Track, Find, Compare and Explore FlockData helps you find what you're looking for.

Which customers influence other customer purchases? Which customers who buy beer, also by games? Where did we ship these batch numbers to? Alert me to all trades > $1,000,000 involving Trader Joe on the Sell side? Complex questions don't become complex projects. They become realtime dashboards, alerts and queries that can be produced in a morning.

FlockData is a data mediation service to track lineage of business information into both an ElasticSearch datastore and Neo4J graph database with minimal upfront analysis or effort.

AB assumes you want to aggregate data from your existing system and look at it new ways to uncover insights you didn't know existed. This real-time anaysis saves time and enables you to identify signal from noise with free-form exploration. This is hugely useful when you are not quite sure what you are looking for in your information and are forming hypotheses on the journey to insight.

All your data is stored as industry as standard JSON documents in open-source database accessible to any application that can talk to a website. This lets you take advantage of next generation visulisation tools and existing number crunching tools like Excel.

Turn your information in to an accessible resource. See if our data [Mission](http://www.monowai.com/wiki/pages/viewpage.action?pageId=13172853) aligns with your way of thinking.

##Architecture
FD follows the design concepts of a [Microservice](http://martinfowler.com/articles/microservices.html). As a service it is in fact itself made up of microservices.

![Alt text](https://bitbucket.org/monowai/auditbucket/raw/ae02c715354756b22b7816986899fcf92a81219b/micro-service.png)

FlockData is well suited to Domain Driven Design, ROA & SOA architectures. It integrates sophisticated index technologies and document management logic in to highly scalable JSON databases. The result lets you look at your information in new and innovative ways to discover patterns in information. 

As loosely coupled services become the normal way to build services, tracking the information that flows across these services becomes vital when it comes to analysing issues both technically and from the business experience. FD supports this initatie by using [Event Sourcing](http://martinfowler.com/eaaDev/EventSourcing.html) techniques. FD takes the view that activities spans your systems as a series of events. 

##Datastores
FlockData coordinates and directs information to various highly scalable data sources to achieve its benefits. These teams are genius. 
* [elasticsearch](https://github.com/elasticsearch/elasticsearch)
* [neo4j](https://github.com/neo4j/neo4j)
* [resdis](https://github.com/antirez/redis)
* [riak](http://basho.com/riak/)
* [rabbitmq](https://github.com/rabbitmq/rabbitmq-server)

## Use Cases
* Data Lineage - what data is changing and by whom across your systems
* Information Integration - Connect your data across systems to find patterns
* Free text searching of your business information
* Keep the audit data out of your transaction processing systems and provide users with full history tracking of their data 
* Recommendation engine - With your information connected, you can ask "people who brought this also brought...." questions 
* [Event Sourcing](http://martinfowler.com/eaaDev/EventSourcing.html) in complex integrated environments.
* Discover graphs, search engines and loosely coupled application development.

## Features
### Search
[ElasticSearch](htt://www.elasticsearch.com) is a spectacular search product enabling you to create complex keyword queries and sophisticated aggregations that respond quickly.  Put [Kibana](http://www.elasticsearch.org/overview/kibana/) on the front and you can start to accumulate realtime dashboards that give you the pulse of change in your business. With FD tapping in to your applications, you can offer your staff Full Text Search capabilities across your actual business data - FD makes it possible to search "the latest version" of your business document, i.e. your Customer record, Invoice, Inventory item, whatever; irrespective of what underlying system created it. 

With FlockData maintaining your ElasticSearch database you gain cross-system search capabilities in hours.

### Explore
Neo4J is a graph database that let's you explore your business information in a highly connected graph. FD will build this graph for you by using information tags that you determine to be of value. You can start exploring your enterprise information as a social graph in hours.

## Working with us
More details? Ideas? Integration assistance? General consulting? Want to get started at the [speed of business](http://www.adamalthus.com/blog/2013/06/05/cloud-computing-and-complexity/#more-890)? 

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

