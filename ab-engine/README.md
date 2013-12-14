ab-engine  - Meta-Data interface
===========

Welcome to AuditBucket's ab-engine meta-data service. This service facade combines the significant processing power of REDIS and Neo4J to support tracking and exploration of data.

You only need to interact with ab-engine. By default, ab-engine will also write "the latest" change to your information in to ab-search. Is is also possible to write via ab-engine directly to ElasticSearch and bypass the exploration if your dataset does not have any degree of connection. Some statistical dumps can be like this.

ab-engine exchanges information with ab-search over http or amqp; you control which with the '-Dab.integration' property at boot time.

You can control how you want to connect to neo4j with the '-Dneo4j' command line switch. Either java or rest for embedded to http.

The concepts and calls are explained in the [Audit Service Call Wiki](https://github.com/monowai/auditbucket/wiki/Audit-Service-Calls)


## Installation
Get the source
```
$ git clone https://github.com/monowai/auditbucket
```

Build with dependencies.
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

ToDo: link to GIST of PostMan bundle.

## Interacting with AuditBucket
HTTP, REST and JSON is the lingua franca - surprised? Didn't think so. Download, compile and deploy.

### Security
Note that the user id is 'mike' and the password is '123'. This is basic configuration stuff hacked in to spring-security.xml. I'm sure you can configure your own lovely security domain, or help me out with an OAuth configuration ;)

## Tracking Data
By default, information is tracked in Neo4J and ElasticSearch. You can, at the point of POST, request that the information be only tracked in Neo4j or only ElasticSearch. This depends on your use case. You might be simply tracking event type information that never changes, so simply storing in ElasticSearch is functional enough as the data is not connectable.

## Creating Data
In the examples below, /ab/ represents the application context with the endpoints starting at /v1/. Substitute for whatever server & context is appropriate for your deployment.

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
### Next steps for you!
Figure out how to audit a record from the API documentation. Understand and add tag structures. Process a batch of Audit record. Log a change.

## Configuration
ToDo:
