auditbucket  - Information Profiling Service
===========

Welcome to AuditBucket. This service enables you to track incoming information, find it, compare it and explore it in a data neutral service.

It represents an exploration in to NoSQL technologies and Spring. Notable projects that have been combined to deliver this functionality include

Datastores
* [elasticsearch](https://github.com/elasticsearch/elasticsearch)
* [neo4j](https://github.com/neo4j/neo4j)
* [resdis](https://github.com/antirez/redis)

Spring Framework
* [spring-data-neo4j](https://github.com/SpringSource/spring-data-neo4j)
* [spring-elasticsearch](https://github.com/dadoonet/spring-elasticsearch)
 
We love open source and think these products are amazing. It can take a bit to figure out a good way to evaluate their suitability to process your information. This is where AB comes in. 

The basic principals behind AB are quite straight forward and are centred on ideas long held by me, and probably you, for a number of years. The projects I've combined and document logic I've applied have made the job of getting the functionality up and useful a lot more quickly than I definetly would have been able to do on my own.

Worst case scenario is you'll learn a few things about building graphs and search engines.

## Executive Summary
Track, Find, Kompare and Explore

We need to keep track of information. We are producing more information faster now that at any time in history. Examining patterns in information offers significant business value and insight.

Auditing is often seen as a low value "we'll get to it later" afterthought in a lot of systems. Applications are concerned with with processing information rather than tracking history of information, perhaps with the exception of "who last changed this" at best. This is not enough when forensic levels of anlaysis may be required and can expose companies to unnecessary risk and regulatory penalties. 

AuditBucket looks at the problem as a document oriented information management challenge. Tracking changes to information across your many applications offers a view of what is going on at the information coalface of your business.

AuditBucket does not "own" your databases. These are free for you to explore and enhance using excellent tools like "Kibana", "Linkurious", "Linkurious" and of course your own information management systems. AB simply organises your documents into a consistent way for exploration with out imposing or requiring your existing information systems to change. 

Typically archival projects are run to get eliminate "old" data from transacton systems. AuditBucket enables this information to be preserved and explored in new and exciting ways while freeing up your transactional systems to do what they do best - perform.

As loosley coupled services integrated over a SOA'ish manner become the normal way to build systems tracking the information that flows across these services becomes vital when it comes to debuging issues. AB can help with this using a technique known as Event Sourcing. AB takes the view that an event spans your computer systems and let's you find this information quickly and analyse what happened or changed.  

## Use Cases
### Search
Elasticsearch is a spectacular search product enabling you to create complex key world queries that respond at the speed of business. Put Kibana on the front and you can start to accumulate realtime dashboards that give you the pulse of change in your business. With AB tapping in to your applications, you can offer your staff Full Text Search capabilities across your actual business data - AB makes available to search "the latest version" of your business document, i.e. your Customer record, Invoice, Intentory item, whatever irrespective of what underlying system created it. 

With AB mainataining your ElasticSearch db, you can have cross system search capabilities in hours.

### Explore
Neo4J is another wonderful tool that let's your explore your business information in a highly connected graph. AB will build this graph for you by using information tags that you determine to be of value. You can start exploring your enterprise information as a social graph in hours.


## How does it work?
Look under the hood of an application and you'll see that it sends information over APIs. By tapping in to these APIs "versions" of the information being changed can be logged in AuditBucket for future analysis and tracking trends freeing your processing systems up from the administrative burdon of carrying such information. 

## What is it good for?

* Understand who's changing what across systems
* Follow updates to information across computer systems in realtime
* [Event Sourcing](http://martinfowler.com/eaaDev/EventSourcing.html)
* Assist in implementing compensating transactions across distributed application boundaries
* Free text searching of data changes
* Keeping the auditing information out of your transaction processing system

### Freetext Search
By integrating the "latest" version of data being changed in ElasticSearch, you have powerful enterprise class way of searching all your computer systems for any data value. Like a google search for your proprietary information.

### How do we talk to it?

REST and JSON. Dowload, compile and deploy. Coming soon - a hosted version.

## How to use

AuditBucket is deployed as two highly scalable services
* ab-engine - connects incoming changes in a Graph database and versions the information in Redis
* ab-search - records the latest version of a document audited by ab-engine in ElasticSearch

You only interact with ab-engine. A REST api exists for bother services. You can use an integration layer to control communication between the two, or configure them to talk via RabbitMQ.

ab-search can be configured to co-exist with your ElasticSearch cluster and is basically a microservice to support this activity. It keeps it's information in neatly organised indexes that allow you to easily apply security to the URLs

Get the source
```
$ git clone https://github.com/monowai/auditbucket
```

Build with dependancies.
```
$ mvn install -Dab.integration=http -Dneo4j=java
```

Check out the configuration files in src/test/resources and src/main/resources that allow ElasticSearch to connect to a cluster (if you're running such a thing). Otherwise you can run the JUnit tests. The test configuration connects to Neo4J in embedded mode, while the release configuration assumes the connection to be over  HTTP. This shows you how to connect to both environments, so modify to suit your needs.

Deploy in TomCat or whatever be your favourite container. You can also run the Tomcat executable WAR built by Maven.

Run the tests (you may get a single failure here...)
```
$ mvn -Dtest=Test* test -Dab.integration=http -Dneo4j=java
```

Once you have the .war file installed in your app server, you can start firing off urls to test things.

### Security
Note that the user id is 'mike' and the password is '123'. This is bodgy configuration stuff hacked in to spring-security.xml. I'm sure you'll configure your own lovely security domain, or help me out with an OAuth configuration ;)

## Tracking Data
By default, information is tracked in Neo4J and ElasticSearch. You can, at the point of POST, request that the information be only tracked in Neo4j or only ElasticSearch. This is down to your usecase. You might be simply tracking event type information that never changes, so simply storing in ElasticSearch is functional enough as the data is not connectable.

## Creating Data
Note that in the examples below, /ab/ is the application context. Substitute for whatever context is appropriate for your deployment.

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
