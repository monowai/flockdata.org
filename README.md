[auditbucket](http://auditbucket.com) - Data Mediation Service
===========
## Project Status
Full support for ElasticSearch 1.1 and Neo4j2 in place. API is settled.

Currently working on support for visualization tools in a sub-project we call ab-view.

[CodeFlower](https://monowai.github.io/CodeFlower/)

## How to get going
You need to gain a brief understanding of the two AuditBucket scalable micro services
* [ab-engine](ab-engine/README.md) - track incoming changes - think LogStash for business data  
* [ab-search](ab-search/README.md) - find information tracked by ab-engine into ElasticSearch

## Executive Summary
Track, Find, Compare and Explore

Which customers influence other customer purchases? Which customers who buy beer, also by games? Where did we ship these batch numbers to? Alert me to all trades > $1,000,000 involving Trader Joe on the Sell side? Complex questions don't become complex projects. They become realtime dashboards, alerts and queries that can be produced in a morning.

AuditBucket is a data mediation service to track lineage of business information into both an ElasticSearch datastore and Neo4J graph database with minimal upfront analysis or effort.

AB assumes you want to aggregate data from your existing system and look at it new ways to uncover insights you didn't know existed. This real-time anaysis saves time and enables you to identify signal from noise with free-form exploration. This is hugely useful when you are not quite sure what you are looking for in your information and are forming hypotheses on the journey to insight.

All your data is stored as industry as standard JSON documents in open-source database accessible to any application that can talk to a website. This lets you take advantage of next generation visulisation tools and existing number crunching tools like Excel.

Turn your information in to an accessible resource.

##Architecture
AB follows the design concepts of a [Microservice](http://martinfowler.com/articles/microservices.html). As a service it is in fact itself made up of microservices.

![Alt text](https://bytebucket.org/monowai/auditbucket/raw/ae02c715354756b22b7816986899fcf92a81219b/micro-service.png)

AB is well suited to Domain Driven Design, ROA & SOA architectures. It integrates sophisticated index technologies and document management logic in to highly scalable JSON databases. The result lets you look at your information in new and innovative ways to discover patterns in information. 

As loosely coupled services become the normal way to build services, tracking the information that flows across these services becomes vital when it comes to analysing issues both technically and from the business experience. AB supports this initatie by using [Event Sourcing](http://martinfowler.com/eaaDev/EventSourcing.html) techniques. AB takes the view that activities spans your systems as a series of events. 

##Datastores
AB coordinates and directs information to various highly scalable data sources to achieve its benefits. These teams are genius. 
* [elasticsearch](https://github.com/elasticsearch/elasticsearch)
* [neo4j](https://github.com/neo4j/neo4j)
* [resdis](https://github.com/antirez/redis)
* [riak](http://basho.com/riak/)
* [rabbitmq](https://github.com/rabbitmq/rabbitmq-server)

## What else can I use if for?
* Understand who's changing what data across your application fortresses
* Track changes to domain information across fortress boundaries in realtime
* [Event Sourcing](http://martinfowler.com/eaaDev/EventSourcing.html) in complex integrated environments.
* Track data to support compensating transactions across distributed fortress boundaries
* Free text searching of your business domain data
* Keep audit data out of your transaction processing fortress
* Discover graphs, search engines and loosely coupled application development.

## Features
### Search
[ElasticSearch](htt://www.elasticsearch.com) is a spectacular search product enabling you to create complex keyword queries that respond at the speed of business. Put [Kibana](http://www.elasticsearch.org/overview/kibana/) on the front and you can start to accumulate realtime dashboards that give you the pulse of change in your business. With AB tapping in to your applications, you can offer your staff Full Text Search capabilities across your actual business data - AB makes it possible to search "the latest version" of your business document, i.e. your Customer record, Invoice, Inventory item, whatever; irrespective of what underlying system created it. 

With AB maintaining your ElasticSearch database, you can have cross-system search capabilities in hours.

### Explore
Neo4J is another wonderful tool that let's your explore your business information in a highly connected graph. AB will build this graph for you by using information tags that you determine to be of value. You can start exploring your enterprise information as a social graph in hours.


## Working with us
More details? Ideas? Integration assistance? General consulting? Want to get started at the [speed of business](http://www.adamalthus.com/blog/2013/06/05/cloud-computing-and-complexity/#more-890)? 

Drop us a line over at [auditbucket.com](http://auditbucket.com/contact-auditbucket/) and we'd be happy to see if we can help you reach your goals. Usual social media channels also apply - [LinkedIn](http://www.linkedin.com/company/3361595) .

##People
We are standing at a pivotal point in the way we manage and process information.
A big thanks to the people who inspired me over the last few years to look at business and technology in a different light 
* [Col Perks](http://www.linkedin.com/pub/col-perks/5/416/b3b)
* [Thoughtworks](http://www.thoughtworks.com) in general and [Martin Fowler](http://martinfowler.com/) in particular
* [Michael Hunger](http://stackoverflow.com/users/728812/michael-hunger)
* [Shay Banon](http://www.elasticsearch.com/about/team/)
* [Jim Webber](http://jimwebber.org/)
* [Jonathan Murray](http://www.adamalthus.com/about/)
* [Nigel Dalton](http://www.linkedin.com/in/nigeldalton)
* [Jeremy Snyder](http://entiviti.com)
* StackOverflow contributors - naturally

While the talks, techniques and ideas of these people have inspired AuditBucket, the interpretation and implementation is my own work and this isn't, and shouldn't, be taken as any sign of their endorsement of AB. 

## Contributing
We encourage contributions to AuditBucket from the community. Here’s how to get started.

* Fork the appropriate sub-projects that are affected by your change. Fork this repository if your changes are for release generation or packaging.
* Make your changes and run the test suite.
* Commit your changes and push them to your fork.
* Open pull-requests for the appropriate projects.
* A Bucketeer will review your pull-request, suggest changes, and merge it when it’s ready and/or offer feedback.

Latest code is on the develop branch

To report a bug or issue, please open a new [issue](https://github.com/monowai/auditbucket/issues) against this repository.

### Licensing
AuditBucket is an open source product. We support a Community edition under the GPLv3 license. The Enterprise edition is available under the AGPLv3 license for open source projects otherwise under a commercial license by [contacting](http://auditbucket.com/contact-auditbucket/). Talk to us about clustering as there can be costs involved with the underlying stack.

Have fun and please share your experiences.

-Mike