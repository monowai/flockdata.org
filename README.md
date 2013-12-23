[auditbucket](http://auditbucket.com) - Data Profiling Service
===========

## Executive Summary
Track, Find, Kompare and Explore

Globally we are producing information now faster than at any time in history. Information is vital. Examining patterns in information can offer significant business value and insight. AuditBucket offers a straightforward data to classify and track information from any computer system. Turn **your** information into a knowledge platform.

Auditing (tracking changes to data) is often seen as a low value "we'll get to it later" afterthought in most computer systems, until it's too late. Applications are concerned and optimized to process information, rather than track the history of changing information. Often at best they can answer you "who last changed this?". This is not enough when forensic levels of analysis are required. Not auditing actionable data can expose organizations to unnecessary risk and financial penalties, and in the most extreme case, make organisations appear to be less than "open and co-operative" with regulators.

AuditBucket views the problem as a **domain document** information management challenge. Everything is tracked as JSON data. The end results are that people can leverage sophisticated search capabilities onto latest available business information, then explore its evolution independently of the underlying transaction processing system that produced it. Kind of like **real-time data warehousing** for sales, and a graph for the marketing department to explore **patterns and behavior**.

AuditBucket does not change your data other than storing it as a JSON representation. It does organise your documents in to a consistent envelope to enable exploration and search. You are completely free to explore and enhance it with excellent query tools such as [Kibana](http://www.elasticsearch.org/overview/kibana/), [Linkurious](http://linkurio.us/) , [Keylines](http://keylines.com/) and of course your own information technology systems. 

Typically, archival projects are run to eliminate "old" data from transaction systems which are costly initiatives that introduce on-going costs. AuditBucket enables insight to be preserved and explored in innovative ways freeing up your transactional systems to do what they do best - perform.

## Still with us?
This project represents an exploration into alternative information systems and the technologies that support them with a view to how they could be put to work in a service/api-oriented world that can be mapped onto business processes in a JSON oriented Domain Driven architecture.

AuditBucket (AB) enables you to track incoming information, find it via a search engine, compare versions of the document as it evolves over time, then explore the information as a network graph to find connections or support visualisation strategies. 

AB leans on the brilliant work being done by the open-source community. The open source projects we have selected represent the cutting edge in information management techniques. AB will enable your journey into big data and looks at information in new and exciting ways for very little effort. **Accelerate your evaluation of Neo4J and ElasticSearch**.

## Licensing
AuditBucket is an open source product. We support a Community edition under the GPLv3 license. The Enterprise edition is available under the AGPLv3 license for open source projects otherwise under a commercial license by contacting [the Bucketeers](http://auditbucket.com/contact-auditbucket/). Talk to us about clustering as there may be costs involved.

## Project Status
We're at 0.90. and very close to being feature frozen. Code and API are stable.  Service is likely to evolve to support various deployment configurations only. Version 1.0 will be released early next year on Neo4J 2.0 (currently running on 1.9) and ElasticSearch 1.0. Unless demand requires it, we won't be guaranteeing an upgrade path from 0.90 to 1.0. From 1.0 on upgrades will be fully supported. Let’s see what happens.

We will be introducing Riak as an alternative KV Store to REDIS shortly. It will be configurable as to which you choose to use. Again, data will not be migrated.

## Working with us
More details? Stuck? Integration assistance? General consulting? Want to get started at the [speed of business](http://www.adamalthus.com/blog/2013/06/05/cloud-computing-and-complexity/#more-890)? 

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

