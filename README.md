[auditbucket](http://auditbucket.com) - Data Profiling Service
===========
## Project Status
We're at 0.91 and very close to being feature frozen. Code and API are stable with only a few changes to the way tags are handled.  Master now supports Neo4j2. There is a Neo4j19 branch if you prefer. Data migration between versions is not guaranteed until we hit 1.0.

While we are supporting Neo4j2 this code should be considered experimental until the SDN4J team finalise their release

## Executive Summary
Track, Find, Compare and Explore

AuditBucket is a mediation service that preserves the business context of your information and creates you an ElasticSearch search engine and Neo4J graph database with very little upfront analysis or effort.

You already know *what* information is crucial to your organisation - you've built and brought applications to capture that store that data. These applications are capturing information in a business context in order to store it for you - orders, customers, interactions etc. Let's take it to the next level. What if you could preserve the business context of the information you are already gathering to create insight. Now what if you could do that across your applications?

By preserving the business context of your information, you can easily establish high performance platforms from which deep insight can be extracted from your data. Search for any word across your data without caring if it is a Product, Address, Company or Person keyword. Summarise the results into beautiful real-time dashboards to keep your finger on the pulse. Explore the associative relationships of your information in innovative ways to identify patterns and trends.

Track distribution dockets of product involving specific batch numbers? Done. Alert me to all trades > 1,000,000 involving Trader X on the Sell side? Done. Complex questions don't become complex projects. They become realtime dashboards, alerts and queries that can be produced in a morning.

The latest generation of information management technologies make working with information fun and easy and AuditBucket makes them effortless to create.

## Still with us?
This project represents an exploration into alternative information systems and the technologies that support them with a view to how they could be put to work in a service/api-oriented world that can be mapped onto business processes in a JSON oriented Domain Driven architecture.

AuditBucket (AB) enables you to track incoming information, find it via a search engine, compare versions of the document as it evolves over time, then explore the information as a network graph to find connections or support visualisation strategies. 

AB leans on the brilliant work being done by the open-source community. The open source projects we have selected represent the cutting edge in information management techniques. AB will enable your journey into big data and looks at information in new and exciting ways for very little effort. **Accelerate your evaluation of Neo4J and ElasticSearch**.

## Licensing
AuditBucket is an open source product. We support a Community edition under the GPLv3 license. The Enterprise edition is available under the AGPLv3 license for open source projects otherwise under a commercial license by contacting [the Bucketeers](http://auditbucket.com/contact-auditbucket/). Talk to us about clustering as there may be costs involved with the underlying stack.

## Working with us
More details? Ideas? Integration assistance? General consulting? Want to get started at the [speed of business](http://www.adamalthus.com/blog/2013/06/05/cloud-computing-and-complexity/#more-890)? 

Drop us a line over at [auditbucket.com](http://auditbucket.com/contact-auditbucket/) and we'd be happy to see if we can help you reach your goals. Usual social media channels also apply - [LinkedIn](http://www.linkedin.com/company/3361595) .

### Contributing
We encourage contributions to AuditBucket from the community. Here’s how to get started.

* Fork the appropriate sub-projects that are affected by your change. Fork this repository if your changes are for release generation or packaging.
* Make your changes and run the test suite.
* Commit your changes and push them to your fork.
* Open pull-requests for the appropriate projects.
* A Bucketeer will review your pull-request, suggest changes, and merge it when it’s ready and/or offer feedback.

To report a bug or issue, please open a new [issue](https://github.com/monowai/auditbucket/issues) against this repository.

##Architecture
The basic principals behind AB are well suited to Domain Driven Design, ROA & SOA architectures. We integrate sophisticated index technologies and document management logic to scalable JSON databases. This lets you look at your information in new and innovative ways.  

As loosely coupled services become the normal way to build systems, tracking the information that flows across these services becomes vital when it comes to analysing issues both technically and from the business experience. AB can help, using a technique known as Event Sourcing. AB takes the view that an event spans your systems and lets you find this information quickly and analyse what happened or changed. 

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

While the talks, techiques and ideas of these people have inspired AuditBucket, the interpretation and implementation is my own work and this isn't, and shouldn't, be taken as any sign of their endorsement of AB. 

## Use Cases
### Search
ElasticSearch is a spectacular search product enabling you to create complex keyword queries that respond at the speed of business. Put Kibana on the front and you can start to accumulate realtime dashboards that give you the pulse of change in your business. With AB tapping in to your applications, you can offer your staff Full Text Search capabilities across your actual business data - AB makes it possible to search "the latest version" of your business document, i.e. your Customer record, Invoice, Inventory item, whatever; irrespective of what underlying system created it. 

With AB maintaining your ElasticSearch database, you can have cross-system search capabilities in hours.

### Explore
Neo4J is another wonderful tool that let's your explore your business information in a highly connected graph. AB will build this graph for you by using information tags that you determine to be of value. You can start exploring your enterprise information as a social graph in hours.

## How does it work?
Look under the hood of an application and you'll see that it sends information over APIs. By tapping in to these APIs, "versions" of the information being changed can be logged in AuditBucket for future analysis and tracking trends. This frees up your processing systems from the administrative burden of carrying such information. 

##Datastores
AB coordinates and directs information to various highly scalable data sources to achieve its benefits. These teams are genius. 
* [elasticsearch](https://github.com/elasticsearch/elasticsearch)
* [neo4j](https://github.com/neo4j/neo4j)
* [resdis](https://github.com/antirez/redis)
* [riak](http://basho.com/riak/)
* [rabbitmq](https://github.com/rabbitmq/rabbitmq-server)

##Frameworks
These guys are connecting the world of today and tomorrow
* [spring-data-neo4j](https://github.com/SpringSource/spring-data-neo4j)
* [spring-elasticsearch](https://github.com/dadoonet/spring-elasticsearch)
* [jedis](https://github.com/xetorthio/jedis) - Driver for REDIS
* [spring](http://spring.io/) - Spring Integration, Spring MVC, Spring Security etc...


## What is it good for?
* Understand who's changing what data across your application fortresses
* Track changes to domain information across fortress boundaries in realtime
* [Event Sourcing](http://martinfowler.com/eaaDev/EventSourcing.html) in complex integrated environments.
* Track data to support compensating transactions across distributed fortress boundaries
* Free text searching of your business domain data
* Keep audit data out of your transaction processing fortress
* Discover graphs, search engines and loosley coupled application development. 

## How to use
You need to gain a brief understanding of the two AuditBucket scalable services
* [ab-engine](ab-engine/README.md) - connects incoming changes in a Graph database (Neo4j) view versions 
* [ab-search](ab-search/README.md) - records the latest version of a document audited by ab-engine in ElasticSearch

## Viewing the information
AuditBucket information is stored in industry standard JSON documents. Any Neo4j or ElasticSearch query tool will work effortlessely with AuditBucket information. 

Have fun and please share your experiences.

-Mike

