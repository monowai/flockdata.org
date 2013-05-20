auditbucket  - Event Sourcing Service
===========

Welcome to AuditBucket. This service enables you to store data information changes in a standalone service

It represents my exploration in to NoSQL technologies and Spring. Notable projects that have combined to deliver this functionality include

* [spring-elasticsearch](https://github.com/dadoonet/spring-elasticsearch)
* [elasticsearch](https://github.com/elasticsearch/elasticsearch)
* [spring-data-neo4j](https://github.com/SpringSource/spring-data-neo4j)
* [neo4j](https://github.com/neo4j/neo4j)

The basic principal is quite straight forward and is based on ideas I've had for a number of years. The frameworks I've combined just make the job of implementing it that much easier to implement something a lot more functional than I might otherwise have been able to do on my own.

## Executive Summary
We need to keep track of information. We are producing more information faster now that at any time in history. Examining patterns in information offers significant business value and insight.

Auditing is often seen as a low value "we'll get to it later" approach in a lot of systems. Applications are concerned with what changed internally at best and tracking no history of changes other than "who last changed it" at worst. This is often not enough when forensic levels of anlaysis may be required and can expose companies to unnecessary risk.

## What is it good for?

* Understand who's changing what across systems
* Follow updates to information across computer systems in realtime
* [Event Sourcing](http://martinfowler.com/eaaDev/EventSourcing.html)
* * Assist in implementing compensating transactions across distributed application boundaries
* Free text searching of data changes
* Keeping the auditing information out of your transaction processing system

## How does it work?

REST and JSON.

## Where's it at?
Very much alpha state. Functionally the API is still undergoing refinement and very little attention has been made to optimizing code or the use of the underlying libraries. I'm getting to that as I can.

Currently, you can run this as a service and create Audit Headers and associated Audit Logs. I'll soon be uploading some example PostMan scripts. The EndPoints package contain the Audit and Registration interfaces

In the meantime, perhaps it's best to treat this as an example in using the technology stack and structuring a project to take advantage of it. 

I hope to finalise the API over the coming few weeks and would welcome feedback and assistance on acheiving this.

## How to use

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

### Creating Data
Register yourself with an account
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
curl -u mike:123 -X PUT  http://localhost:8080/ab/fortress/MyFortressName
```
### Create an Audit Header for the Fortress
```
curl -u mike:123 -X PUT http://localhost:8080/ab/audit/header/new/ -d '"fortress":"MyFotressName", "fortressUser": "yoursystemuser", "recordType":"Company","when":"2012-11-10", yourRef:"123"}'
```
Result code is a UID that your system can store and must use to create a log record in the next section 

### Create a log for an Audit Header
```
curl -u mike:123 -H "Content-Type:application/json" -X PUT http://localhost:8080/ab/audit/log/ -d '{"eventType":"change","auditKey":"c27ec2e5-2e17-4855-be18-bd8f82249157","fortressUser":"miketest","when":"2012-11-10", "what": "{\"name\": \"val\"}" }'
```
