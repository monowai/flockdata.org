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

Clone the repo. 


* Register yourself with an account
* Create an Application Fortress 
* Create an Audit Log for the Fortress
* Create a log





