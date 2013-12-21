ab-engine  - Meta-Data API
===========
Welcome to AuditBucket's ab-engine meta-data service. This service facade coordinates KV Stores and Neo4J to support tracking and exploration of data. While Redis is the current default, we are working to add Riak support shortly.

You only need to interact with ab-engine. By default, ab-engine will also write "the latest" change to your information in to [ab-search](../ab-search). Is is also possible to write via ab-engine directly to ElasticSearch and bypass the exploration if your dataset does not have any degree of connection. Some statistical dumps can be like this.

You can control how you want to connect to neo4j with the '-Dneo4j' command line switch. Either java or rest for embedded or http respectively.

When ab-engine runs embedded Neo4J the REST api is exposed on port 7474 so exploration tools continue to work. You can also interact with the database via neo4j-shell as part of the standard community distribution of Neo4J. 

The concepts and calls are explained in the [Audit Service Call Wiki](https://github.com/monowai/auditbucket/wiki/Audit-Service-Calls)

## Dependencies
Only mandatory dependency is REDIS as at this time. Install this separately. We assume you are running all of this on your developer workstation on default ports. If this is the case, then you are good to go

## Installation
Get the source
```
$ git clone https://github.com/monowai/auditbucket
```

Build with dependencies, including running the tests
```
$ mvn install -Dab.integration=http -Dneo4j=java
```

Run the tests
```
$ mvn -Dtest=Test* test -Dab.integration=http -Dneo4j=java
```

## Configuration
ab-engine exchanges information with ab-search over http or amqp integration; you control which with the '-Dab.integration' property at boot time.

[Spring Configuration](src/main/webapp/WEB-INF/spring) will give you an overview as to the various default configurations and properties that can be set. These can be overridden while ab-engine is starting if you pass them in via the command line. But for evaluation purposes, they are good enough!

If you want to use AMQP, and we suggest you do, the default message platform we support is [RabbitMQ](http://www.rabbitmq.com/). Just download and install and you're good to go. Dead simple. If you want to support a different messaging platform, then check out the integration*.xml files for the pattern, add your own and contribute!

Note that if ab-engine is integrating via AMQP, then ab-search must use this approach as well. 

## Container Deployment
While this all looks rather technical, it is simply a matter of getting the WAR file deployed in a webserver. There is no "seperate database" unless you wish to configure AB to talk to one.

Deploy in TomCat or whatever be your favourite container. Maven will build an executable tomcat 7 package for you that you can run from the Java command line. We will assume that you are going to deploy the WAR to TC7 via your IDE.

Default HTTP port for ab-engine is 8080 and for ab-search its 8081. If you are using different ports then review the [configuration files] (src/main/resources/config.properties) that describe how engine and search find each other. If you have an existing ElasticSearch cluster, you will also want to review 

Once you have the .war file installed in your app server, you can start firing off urls to test things.

## Interacting with AuditBucket
HTTP, REST and JSON is the lingua franca - surprised? Didn't think so. Download, compile and deploy.

### Security
Note that the user id is 'mike' and the password is '123'. This is basic configuration stuff hacked in to spring-security.xml. I'm sure you can configure your own lovely security domain, or help me out with an OAuth configuration ;)

## Tracking Data
By default, information is tracked in Neo4J and ElasticSearch. You can, at the point of POST, request that the information be only tracked in Neo4j or only ElasticSearch. This depends on your use case. You might be simply tracking event type information that never changes, so simply storing in ElasticSearch is functional enough as the data is not connectable.

## Creating Data
In the examples below, /ab-engine/ represents the application context with the endpoints starting at /v1/. Substitute for whatever server & context is appropriate for your deployment.

###Register yourself with an account
```
curl -u mike:123 -H "Content-Type:application/json" -X PUT http://localhost:8080/ab-engine/v1/profiles/register -d '{"name":"mikey", "companyName":"Monowai","password":"whocares"}'
```
### See who you are
```
curl -u mike:123 -X GET http://localhost:8080/ab-engine/v1/profiles/me
```
### Create an Application Fortress
This is one of your computer systems that you want to audit
```
curl -u mike:123 -H "Content-Type:application/json" -X PUT  http://localhost:8080/ab-engine/v1/fortress -d '{"name": "SAP-AR","searchActive": true}'
```
### Next steps for you!
Figure out how to audit a record from the API documentation. Understand and add tag structures. Process a batch of Audit records. Log a change.

