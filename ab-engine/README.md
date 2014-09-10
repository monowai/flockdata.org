ab-engine  - Meta-Data API
===========
Welcome to AuditBucket's ab-engine meta-data service. This service facade coordinates KV Stores and Neo4J to support tracking and exploration of data. While Redis is the current default, we are working to add Riak support shortly.

You only need to interact with ab-engine. By default, ab-engine will also write "the latest" change to your information in to [ab-search](../ab-search). Is is also possible to write via ab-engine directly to ElasticSearch and bypass the exploration if your dataset does not have any degree of connection. Some statistical dumps can be like this.

When ab-engine runs embedded Neo4J the REST api is exposed on port 7474 so exploration tools continue to work. You can also interact with the database via neo4j-shell as part of the standard community distribution of Neo4J. 

The concepts and calls are explained in the [Audit Service Call Wiki](https://github.com/monowai/auditbucket/wiki/Audit-Service-Calls)

Please see our [PostMan API gist](https://gist.github.com/monowai/8077021)  for a quick and convenient way of making REST calls to AuditBucket.

## Dependencies
Only mandatory dependency at this time is REDIS or RIAK as a KV store; This should be installed separately according to your OS instructions. It is recommended that you install RabbitMQ in order to use reliable AMQP cooms between ab-engine an ab-search, however if you are just experimenting, then http integration is fine.

Start your supported KV Store

## Installation
Get the source
```
$ git clone https://bitbucket.org/monowai/auditbucket
```

Build with dependencies, including running the tests
```
$ mvn install
```

Run the tests
```
$ mvn test
```

You may see an error suggesting that RIAK could not be found. This can be ignored or remedied by starting RIAK.

## Configuration
ab-engine exchanges information with ab-search over http or amqp integration; you control which with the '-Dab.integration' property at boot time.

[Spring Configuration](src/main/webapp/WEB-INF/spring) will give you an overview as to the various default configurations and properties that can be set. These can be overridden while ab-engine is starting if you pass them in via the command line. But for evaluation purposes, they are good enough!

If you want to use AMQP, and we suggest you do, the default message platform we support is [RabbitMQ](http://www.rabbitmq.com/). Just download and install and you're good to go. Dead simple. If you want to support a different messaging platform, then check out the integration*.xml files for the pattern, add your own and contribute!

Note that if ab-engine is integrating via AMQP then ab-search must use this approach as well.

## Start Your Engines
You can either deploy ab-engine to an existing servlet container or run it standalone.

Run ab-engine (with Spring security configuration) straight from the ab-engine/target folder with the following command
```
$ cd ab-engine/target
$ java -jar ab-engine-war.jar -Dneo4j=java -Dab.integration=http -Dab.auth.provider=simple -httpPort=8080 -Dab.config=./classes/config.properties -Dlog4j.configuration=file:./classes/log4j.xml
```

Run ab-engine (with Stormpath security configuration) straight from the ab-engine/target folder with the following command
```
$ cd ab-engine/target
$ java -jar ab-engine-war.jar -Dneo4j=java -Dab.integration=http -Dab.auth.provider=stormpath -Dab.auth.config=./classes/stormpath.properties -httpPort=8080 -Dab.config=./classes/config.properties -Dlog4j.configuration=file:./classes/log4j.xml
```

Deploy in TomCat or whatever be your favourite container. Maven will build an executable Tomcat7 package for you that you can run from the Java command line. We will assume that you are going to deploy the WAR to TC7 via your IDE.

Default HTTP port for ab-engine is 8080 and for ab-search its 8081. If you are using different ports then review the [configuration files] (src/main/resources/config.properties) that describe how engine and search find each other. If you have an existing ElasticSearch cluster, you will also want to review 

Once you have the .war file installed in your app server, you can start firing off urls to test things.

## Interacting with AuditBucket
HTTP, REST and JSON is the lingua franca.

### Authorisation
AuditBucket supports 2 options in terms of security configuration.
Default is Spring security. This is chosen by setting -Dab.auth.provider=simple (case sensitive) 
Note that the user id is under simple=security is 'batch' and the password is '123'. This simple configuration can be found in simple-security.xml.

Optionally, we can start AuditBucket with Stormpath as the security provider.
This is chosen by setting -Dab.auth.provider=stormpath (case sensitive) while booting ab-engine. stormpath.properties holds customization properties related to Stormpath. The properties that can be customized are as follows
ab.auth.stormpath.apiKeyFileLocation - Path where Stormpath API Key file is located. This is needed for handshake for handshake between AuditBucket and Stormpath
ab.auth.stormpath.application - URL of the application that has been setup in Stormpath.
ab.auth.stormpath.group.user - URL of User group setup in Stormpath
ab.auth.stormpath.group.admin - URL of Admin group setup in Stormpath

Once customized, the stormpath.properties path needs to be passed via -Dab.auth.config property.

You are free to configure your own security domain, or help us out with an OAuth configuration ;)

## Tracking Data
By default, information is tracked in Neo4J and ElasticSearch. You can, at the point of POST, request that the information be only tracked in Neo4j or only ElasticSearch. This depends on your use case. You might be simply tracking event type information that never changes, so simply storing in ElasticSearch is functional enough as the data is not require the meshing of connections.

## Creating Data
In the examples below, /ab-engine/ represents the application context with the endpoints starting at /v1/. Substitute for whatever server & context is appropriate for your deployment.

###Register yourself with an account
```
curl -H "Content-Type:application/json" -X POST http://localhost:8080/ab-engine/v1/profiles/ -d '{"name":"batch", "companyName":"Monowai","login":"whocares"}'
```
### See who you are
```
curl -u batch:123 -X GET http://localhost:8080/ab-engine/v1/profiles/me/
```
### Create an Application Fortress
This is one of your computer systems that you want to audit
```
curl -u batch:123 -H "Content-Type:application/json" -X POST http://localhost:8080/ab-engine/v1/fortress/ -d '{"name": "demo-app","searchActive": true}'
```
### Track a Data Event
You should have started [ab-search](../ab-search) before doing this if you're not using RabbitMQ otherwise expect a communications error!
```
curl -u batch:123 -H "Content-Type:application/json" -X POST http://localhost:8080/ab-engine/v1/track/ -d '{
  "fortress":"demo-app", 
  "event":"Create",
  "documentType":"Debtor",
  "callerRef":"myRef",
  "log": {   "fortressUser": "ak0919",
           		 "what": {"BusinessData": "Your Text", "nestedObject": {"serviceMessage": "kool for kats"}}
  }
}'
```
### Find that doc in ElasticSearch
Note that ab-search is configured by default to have ElasticSearch listening on port 9201. This command is an ElasticSearch query, not an ab-search one.

```
curl -H "Content-Type:application/json" -X POST http://localhost:9201/ab.*/_search -d '{
    query: {
          query_string : {
            "query" : "debtor"
           }
      }
}'
````

### Next steps....
Figure out how to audit a record from the [API](http://www.monowai.com/wiki/pages/viewpage.action?pageId=13172790) documentation. Understand and add [Tags](http://www.monowai.com/wiki/pages/viewpage.action?pageId=13172831). Process a batch of changes. Log a single change to an existing header.

You will want to look at the various Bean packages in [ab-common](https://bitbucket.org/monowai/auditbucket/src/abdb12458b5537567546aa2ba6ffe01bc83cc521/ab-common/?at=develop) to find the various properties you can set while we continue to enhance the documents.

