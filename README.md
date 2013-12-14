auditbucket  - Information Profiling Service
===========

## Executive Summary
Track, Find, Kompare and Explore

Information is power. We are producing information now faster than at any time in history. Examining patterns in information can offer significant business value and insight. AuditBucket offers a straightforward data agnostic way to classify and track information from any computer system.

Auditing (tracking changes to data) is often seen as a low value "we'll get to it later" afterthought in most computer systems, until it's too late. Applications are concerned and optimized to process information rather than track the history of changing information. Often at best they can answer you "who last changed this?". This is not enough when forensic levels of anlaysis are required. Not auditing actionable data can expose organizations to unnecessary risk and financial penalties, perhaps even make your organisation appear to be less than "open and co-operative" with regulators. 

AuditBucket looks at the problem as a document-oriented information management challenge. The end results are that people can leverage sophisticated search capabilities on to latest available business information then explore it's evolution independently of your transaction processing systems. Kind of like real-time data warehousing for sales and a graph for the marketing department to mine.

AuditBucket does not change your data in anyway. It enriches it leaving you free to explore and enhance with excellent query tools such as [Kibana](http://www.elasticsearch.org/overview/kibana/), [Linkurious](http://linkurio.us/) , [Keylines](http://keylines.com/) and of course your own information technology systems. AB simply organises your documents into a consistent way for exploration and search without imposing or requiring your existing information technology systems to change. 

Typically archival projects are run to eliminate "old" data from transaction systems which can compound problems and introduce on-going costs. AuditBucket enables insight to be preserved and explored in innovative ways, freeing up your transactional systems to do what they do best - perform.

## Still with us?
This project represents an exploration into alternative information systems and the technologies that support them with a view to how they could be put to work in a service/api-oriented world that can be mapped on to business processes. 

AuditBucket(AB) enables you to track incoming information, find it via a search engine, compare versions over time, and explore it as a network graph to find connections in your information. Tracking changes to information across your many applications offers a view of what is going on at the information interface of your business.

AB leans on the brilliant work being done by the open-source community. The open source projects we have selected represent the cutting edge in information management techniques. AB will enable you to evaluate big data and look at information in new and exciting ways for very little effort - particularly given the range of technologies that AB ties together.  

## Licensing
AuditBucket is an open source product. We support a Community edition under the GPLv3 license. The Enterprise edition is available under the AGPLv3 license for open source projects otherwise under a commercial license by contacting [the bucketeers](http://auditbucket.com/contact-auditbucket/). Talk to us about clustering as there may be costs involved.

## Working with us
More details? Stuck? Integration assistance? General consulting? Want to get started at the [speed of business](http://www.adamalthus.com/blog/2013/06/05/cloud-computing-and-complexity/#more-890)? 

Drop us a line over at [auditbucket.com](http://auditbucket.com/contact-auditbucket/) and we'd be happy to see if we can help you reach your goals. Usual social media channels also apply.

##Architecture
The basic principals behind AB are well suited to Domain Driven Design, ROA & SOA architectures. We integrate sophisticated index technologies and document management logic to scalable JSON databases. This lets you look at your information in new and innovative ways.  

As loosely coupled services become the normal way to build systems, tracking the information that flows across these services becomes vital when it comes to analysing issues both technically and from the business experience. AB can help, using a technique known as Event Sourcing. AB takes the view that an event spans your systems and lets you find this information quickly and analyse what happened or changed. 

##Datastores
AB coordinates and directs information to various highly scalable data sources to achieve its benefits. These teams are genius. 
* [elasticsearch](https://github.com/elasticsearch/elasticsearch)
* [neo4j](https://github.com/neo4j/neo4j)
* [resdis](https://github.com/antirez/redis)
* [rabbitmq](https://github.com/rabbitmq/rabbitmq-server)

##Frameworks
These guys are connecting the world of today and tomorrow
* [spring-data-neo4j](https://github.com/SpringSource/spring-data-neo4j)
* [spring-elasticsearch](https://github.com/dadoonet/spring-elasticsearch)
* [jedis](https://github.com/xetorthio/jedis) - Driver for REDIS
* [spring](http://spring.io/) - Spring Integration, Spring MVC, Spring Security etc...

##People
We are standing at a pivotal point in the way we manage and process information.
A big thanks to the people who helped me look at technology in a different light over the last few years. 
* Col Perx
* Martin Fowler
* Michael Hunger
* Shay Banon
* Jim Webber
* StackOverflow !

## Use Cases
### Search
ElasticSearch is a spectacular search product enabling you to create complex keyword queries that respond at the speed of business. Put Kibana on the front and you can start to accumulate realtime dashboards that give you the pulse of change in your business. With AB tapping in to your applications, you can offer your staff Full Text Search capabilities across your actual business data - AB makes it possible to search "the latest version" of your business document, i.e. your Customer record, Invoice, Inventory item, whatever; irrespective of what underlying system created it. 

With AB maintaining your ElasticSearch database, you can have cross-system search capabilities in hours.

### Explore
Neo4J is another wonderful tool that let's your explore your business information in a highly connected graph. AB will build this graph for you by using information tags that you determine to be of value. You can start exploring your enterprise information as a social graph in hours.

## How does it work?
Look under the hood of an application and you'll see that it sends information over APIs. By tapping in to these APIs, "versions" of the information being changed can be logged in AuditBucket for future analysis and tracking trends. This frees up your processing systems from the administrative burden of carrying such information. 

## What is it good for?
* Understand who's changing what data across your systems
* Track updates to information across computer systems in realtime
* [Event Sourcing](http://martinfowler.com/eaaDev/EventSourcing.html) in complex integrated environments.
* Assist in implementing compensating transactions across distributed application boundaries
* Free text searching of your business information
* Keeping the auditing information out of your transaction processing system
* Discover graphs, search engines and loosley coupled application development. 

### Freetext Search
By integrating the "latest" version of data being changed in ElasticSearch, you have a powerful enterprise class way of searching all your computer systems for any data value. It's like a Google search for your proprietary information.

## How to use
You need to gain a brief understanding of the two AuditBucket scalable services
* [ab-engine](https://github.com/monowai/auditbucket/tree/master/ab-engine) - connects incoming changes in a Graph database (Neo4j) and versions the information in Redis
* [ab-search](https://github.com/monowai/auditbucket/tree/master/ab-search) - records the latest version of a document audited by ab-engine in ElasticSearch

## Viewing the information
AuditBucket information is stored in industry standard JSON documents. Any Neo4j or ElasticSearch query tools can work effortlessely AuditBucket information. 


-Mike
