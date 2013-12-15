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

AuditBucket indexes documents in a structure that follows [ab.{fortress-code}.{document-type}/{caller-ref}] structure.

This lets you search wide or narrow index structures with wild cards. This approach keeps your syslog activity from logstash seperate from your business domain data in the same cluster. Neat eh?

### Freetext Search
By integrating the "latest" version of data being changed in ElasticSearch, you have a powerful enterprise class way of searching all your computer systems for any data value. It's like a Google search for your domain data.

### Next steps for you!
Figure out how to query ElasticSearch. Deploy Kibana and start looking at your data in new ways.

## Configuration
See config.properties for the ab-search defaults. These can be overridden while ab-search is starting if you pass them in via the command line. But for evaluation purposes, they are good enough!

Note that if ab-search is integrating via AMQP, then ab-engine must use this approach as well. 

If you want to use AMQP, and we suggest you do, the default message platform we support is [RabbitMQ](http://www.rabbitmq.com/). Just download and install and you're good to go. Dead simple. If you want to support a different messaging platform, then check out the integration*.xml files for the pattern, add your own and contribute!

ToDo:
