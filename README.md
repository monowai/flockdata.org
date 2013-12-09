auditbucket  - Information Profiling Service
===========

Welcome to AuditBucket. This project enables you to track incoming information, find it via a search engine, compare it and track its changes over time, and explore it in a network graph to find connections.

AuditBucket (AB) represents a sophisticated exploration in NoSQL technologies and Spring for a service-oriented world. As with most modern projects, we lean on the brilliant work being done by the open-source community as a whole. We believe AB will enable you to explore big data and look at information in new and exciting ways for very little effort - particularly given the range of technologies that AB ties together.  

##Datastores
AB coordinates and directs information to various highly scalable data sources to achieve its benefits. 
* [elasticsearch](https://github.com/elasticsearch/elasticsearch)
* [neo4j](https://github.com/neo4j/neo4j)
* [resdis](https://github.com/antirez/redis)
* [rabbitmq](https://github.com/rabbitmq/rabbitmq-server)

##Frameworks
We can't build much with out standing on the shoulders of giants. 
* [spring-data-neo4j](https://github.com/SpringSource/spring-data-neo4j)
* [spring-elasticsearch](https://github.com/dadoonet/spring-elasticsearch)
* [jedis](https://github.com/xetorthio/jedis) - Driver for REDIS
* [spring](http://spring.io/) - Spring Integration, Spring MVC, Spring Security etc...
 
We love open source and think all of the above products are amazing, representing the cutting edge in information management techniques. However, it can take a bit of effort to figure out a good way to evaluate their suitability to process your information. This is where AuditBucket can help you, tying together the strengths of each.

## Executive Summary
Track, Find, Kompare and Explore

We need to keep track of information. We are producing more information faster now that at any time in history. Examining patterns in information offers significant business value and insight.

Auditing (tracking that data) is often seen as a low value "we'll get to it later" afterthought in a lot of systems. Applications are concerned with with processing information rather than tracking the history of information, perhaps with the exception of "who last changed this" at best. This is not enough when forensic levels of anlaysis are required. Not auditing data can expose organizations to unnecessary risk and regulatory penalties. 

AuditBucket looks at the problem as a document-oriented information management challenge. Tracking changes to information across your many applications offers a view of what is going on at the information interface of your business.

AuditBucket does not "own" your data. You are free to explore and enhance your views using excellent tools like "Kibana", "Linkurious", "Linkurious" and of course your own information management systems. AB simply organises your documents into a consistent way for exploration without imposing or requiring your existing information systems to change. 

Typically archival projects are run to get eliminate "old" data from transacton systems. AuditBucket enables this information to be preserved and explored in new and exciting ways while freeing up your transactional systems to do what they do best - perform.

## Licensing
AuditBucket is an open source product. We support a Community edition under the GPLv3 license. The Enterprise edition is available under the AGPLv3 license for open source projects otherwise under a commercial license by contacting [the team](http://auditbucket.com/contact-auditbucket/). Talk to us about clustering as there may be costs involved.

##Architecture
The basic principals behind AB are well suited to Domain Driven Design and SOA architectures. We connect sophisticated index technologies and document management logic to scalable databases. This lets you look at your information in new and exciting ways.  

As loosely coupled services, integrated in SOA approaches, become the normal way to build systems, tracking the information that flows across these services becomes vital when it comes to debugging issues. AB can help, using a technique known as Event Sourcing. AB takes the view that an event spans your systems and lets you find this information quickly and analyse what happened or changed. 

## Use Cases
### Search
ElasticSearch is a spectacular search product enabling you to create complex keyword queries that respond at the speed of business. Put Kibana on the front and you can start to accumulate realtime dashboards that give you the pulse of change in your business. With AB tapping in to your applications, you can offer your staff Full Text Search capabilities across your actual business data - AB makes it possible to search "the latest version" of your business document, i.e. your Customer record, Invoice, Inventory item, whatever; irrespective of what underlying system created it. 

With AB maintaining your ElasticSearch database, you can have cross-system search capabilities in hours.

### Explore
Neo4J is another wonderful tool that let's your explore your business information in a highly connected graph. AB will build this graph for you by using information tags that you determine to be of value. You can start exploring your enterprise information as a social graph in hours.

## How does it work?
Look under the hood of an application and you'll see that it sends information over APIs. By tapping in to these APIs, "versions" of the information being changed can be logged in AuditBucket for future analysis and tracking trends. This frees up your processing systems from the administrative burden of carrying such information. 

## What is it good for?

* Understand who's changing dta what across systems
* Follow updates to information across computer systems in realtime
* [Event Sourcing](http://martinfowler.com/eaaDev/EventSourcing.html)
* Assist in implementing compensating transactions across distributed application boundaries
* Free text searching of data changes
* Keeping the auditing information out of your transaction processing system
* Learning about building graphs, search engines and loosley coupled application development. 

### Freetext Search
By integrating the "latest" version of data being changed in ElasticSearch, you have a powerful enterprise class way of searching all your computer systems for any data value. It's like a Google search for your proprietary information.

## How to use
AuditBucket is deployed as two highly scalable services
* ab-engine - connects incoming changes in a Graph database (Neo4j) and versions the information in Redis
* ab-search - records the latest version of a document audited by ab-engine in ElasticSearch

You only interact with ab-engine. A REST api exists for other services. You can use an integration layer to control communication between the two, or configure them to talk via RabbitMQ.

ab-search can be configured to co-exist with your ElasticSearch cluster and is basically a micro-service to support this activity. It stores information in neatly organised indexes that allow you to easily apply security to the URLs

Get the source
```
$ git clone https://github.com/monowai/auditbucket
```

Build with dependancies (note the command line parameters, these allow appropriate config files to be loaded at boot).
```
$ mvn install -Dab.integration=http -Dneo4j=java
```
Check out the configuration files in src/test/resources and src/main/resources that allow ElasticSearch to connect to a cluster (if you're running one). Otherwise you can run the JUnit tests. The test configuration connects to Neo4J in embedded mode, while the release configuration assumes the connection to be over  HTTP. This shows you how to connect to both environments, so modify to suit your needs.

Deploy in TomCat or your favourite container. You can also run the Tomcat executable WAR built by Maven.

Run the tests (you may get a single failure here...)
```
$ mvn -Dtest=Test* test -Dab.integration=http -Dneo4j=java
```

Once you have the .war file installed in your app server, you can start firing off urls to test things.

### Security
Note that the user id is 'mike' and the password is '123'. This is basic configuration stuff hacked in to spring-security.xml. I'm sure you can configure your own lovely security domain, or help me out with an OAuth configuration ;)

## Tracking Data
By default, information is tracked in Neo4J and ElasticSearch. You can, at the point of POST, request that the information be only tracked in Neo4j or only ElasticSearch. This depends on your use case. You might be simply tracking event type information that never changes, so simply storing in ElasticSearch is functional enough as the data is not connectable.

## Interacting with AuditBucket
HTTP, REST and JSON is the lingua franca - surprised? Didn't think so. Download, compile and deploy. 

In the examples below, /ab/ is the application context. Substitute for whatever context is appropriate for your deployment.

###Register yourself with an account
```
curl -u mike:123 -H "Content-Type:application/json" -X PUT http://localhost:8080/ab/v1/profiles/register -d '{"name":"mikey", "companyName":"Monowai","password":"whocares"}'
```
### See who you are
```
curl -u mike:123 -X GET http://localhost:8080/ab/v1/profiles/me
```
### Create an Application Fortress 
This is one of your computer systems that you want to audit
```
curl -u mike:123 -H "Content-Type:application/json" -X PUT  http://localhost:8080/ab/v1/fortress -d '{"name": "SAP-AR","searchActive": true}'
```
Ask and we'll provide a GIST of PostMan calls that show all the API calls and parameters.
Please review the [Audit Service Calls](https://github.com/monowai/auditbucket/wiki/Audit-Service-Calls) for further information and detailed syntax

## Viewing the information
Any Neo4J or ElasticSearch query product will work with AuditBucket information. 

[Kibana](http://www.elasticsearch.org/overview/kibana/) - dashboard/query tool from the clever chaps at ElasticSearch
[Linkurious](http://linkurio.us/) - HTML graph exploration tool
