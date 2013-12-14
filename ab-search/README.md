ab-search - ElasticSearch Facade
===============

This service is used by ab-engine to talk to ElasticSearch or notionally any search product you would care to write a handler for. It listens for data integrating over http or amqp puts the docs into ElasticSearc

There is a ping url

```
curl -X GET http://localhost:8081/ab-search/v1/ping
```

And health of the service can be queried via a secured call

```
curl -u mike:123 -X GET http://localhost:8081/ab-search/v1/health
```

Generally that's it. There are some endpoints implemented using an @ServiceActivator pattern but these are reserved for ab-engine integration.

By default, ab-search spins up and ElasticSearch instance. It uses ports 9301 and 9301. This is basically so you can experiment with ElasticSearch clustering on one computer.

The ElasticSearch cluster that ab-search joins by default is 

```
es.clustername=es_auditbucket
```

You can change this by passing in your own configuration settings, i.e.

```
-Des.clustername=mycluster
```

AuditBucket indexes documents in a structure that follows ab.{fortressCode}.{documenttype}/{callerRef} structure.

This lets you search "all of AB", "all documents in an AB fortress" or any other combination of wild cards you can imagine. So you can keep your syslog activity from logstash seperate from your business domain data in the same cluster. Neat eh?
