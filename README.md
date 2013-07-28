auditbucket  - Data Tracking Service
===========

Welcome to AuditBucket. This service enables you to store data information changes in a standalone service

It represents an exploration in to NoSQL technologies and Spring. Notable projects that have combined to deliver this functionality include

* [spring-elasticsearch](https://github.com/dadoonet/spring-elasticsearch)
* [elasticsearch](https://github.com/elasticsearch/elasticsearch)
* [spring-data-neo4j](https://github.com/SpringSource/spring-data-neo4j)
* [neo4j](https://github.com/neo4j/neo4j)

The basic principal is quite straight forward and is based on ideas I've had for a number of years. The frameworks I've combined just make the job of implementing it that much easier to implement something a lot more functional than I might otherwise have been able to do on my own.

## Executive Summary
We need to keep track of information. We are producing more information faster now that at any time in history. Examining patterns in information offers significant business value and insight.

Auditing is often seen as a low value "we'll get to it later" approach in a lot of systems. Applications are concerned with what changed internally at best and tracking no history of changes other than "who last changed it" at worst. This is often not enough when forensic levels of anlaysis may be required and can expose companies to unnecessary risk. 

AuditBucket looks at the problem as an opportunity. Enabling the tracking of changes in information occuring across your applications offers a view of what is going on at the information coalface of your business.

Typically archival projects are run to get eliminate "old" data from transacton systems. AuditBucket enables this information to be preserved and explored in new and exciting ways.

### How does it work?
Look under the hood of an application and you'll see that it sends information over APIs. By tapping in to these APIs "versions" of the information being changed can be logged in AuditBucket for future analysis and tracking trends freeing your processing systems up from the administrative burdon of carrying such information. 

## What is it good for?

* Understand who's changing what across systems
* Follow updates to information across computer systems in realtime
* [Event Sourcing](http://martinfowler.com/eaaDev/EventSourcing.html)
* Assist in implementing compensating transactions across distributed application boundaries
* Free text searching of data changes
* Keeping the auditing information out of your transaction processing system

## Freetext Search
By integrating the "latest" version of data being changed in ElasticSearch, you have powerful enterprise class way of searching all your computer systems for any data value. Like a google search for your proprietary information.

## How does it work?

REST and JSON.

Fortresses, Audith Headers and logs are stored in a graph to enable efficient analysis of changes as they are made to your systems.

Additionally the "what" data that changed in your system is stored in ElasticSearch (Lucene) so that you can perform free text queries against your systems data!

## Where's it at?
Entering beta state. Functionally the API continues to undergo refinement and very little attention has been made to optimizing code or the use of the underlying libraries. That said, the product has integration stress tests that show things performing very well indeed.

Currently, you can run this as a service and create Audit Headers and associated Audit Logs. I'll soon be uploading some example PostMan scripts. The EndPoints package contain the Audit and Registration interfaces

I hope to finalise the API over the coming few weeks and would welcome feedback and assistance on acheiving this.

## How to use

AuditBucket is two services
* ab-engine - catalogs all changes in a Graph database
* ab-search - records the latest version of a document audited by ab-engine

Typically you only interact with ab-engine. A REST api exists for bother services. You can use an integration layer to control communication between the two, or configure them to talk via RabbitMQ.

ab-search can be configured to talk to your ElasticSearch cluster and is basically a microservice to support this activity. 

Get the source
```
$ git clone https://github.com/monowai/auditbucket
```

Build with dependancies.
```
$ mvn install
```

Check out the configuration files in src/test/resources and src/main/resources that allow ElasticSearch to connect to a cluster (if you're running such a thing). Otherwise you can run the JUnit tests. The test configuration connects to Neo4J in embedded mode, while the release configuration assumes the connection to be over  HTTP. This shows you how to connect to both environments, so modify to suit your needs.

Deploy in TomCat or whatever be your favourite container. (todo: support as a standalone service with Jetty or somesuch)

Run the tests (you may get a single failure here...)
```
$ mvn -Dtest=Test* test
```

Once you have the .war file installed in your app server, you can start firing off urls to test things.

### Security
Note that the user id is 'mike' and the password is '123'. This is bodgy configuration stuff hacked in to spring-security.xml. I'm sure you'll configure your own lovely security domain, or help me out with an OAuth configuration ;)

## Creating Data
Note that in the examples below, /ab/ is the application context. Substitute for whatever context is appropriate for your deployment.

###Register yourself with an account
```
curl -u mike:123 -H "Content-Type:application/json" -X PUT http://localhost:8080/ab/profiles/register -d '{"name":"mikey", "companyName":"Monowai Dev","password":"whocares"}'
```
### See who you are
```
curl -u mike:123 -X GET http://localhost:8080/ab/profiles/me
```
### Create an Application Fortress 
This is one of your computer systems that you want to audit
```
curl -u mike:123 -H "Content-Type:application/json" -X PUT  http://localhost:8080/ab/profiles/fortress/new -d '{"name": "SearchNA","ignoreSearchEngine": false}'
```

Please review the [Audit Service Calls](https://github.com/monowai/auditbucket/wiki/Audit-Service-Calls) for further information and detailed syntax
