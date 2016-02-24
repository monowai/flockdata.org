fd-search - ElasticSearch Facade
===============
This service is used by [fd-engine](../fd-engine) to talk to ElasticSearch or notionally any search product you would care to write a handler for. It listens for data integrating over http or amqp puts the docs into ElasticSearch

## Configuration
See [Spring](src/main/webapp/WEB-INF/spring) and [Application config](src/main/resources/config.properties) for various fd-search defaults. These can be overridden while fd-search is starting if you pass them in via the command line. But for evaluation purposes, they are good enough!

Note that if fd-search is integrating via AMQP, then fd-engine must use this approach as well. 

The ElasticSearch cluster that fd-search joins by default is 

```
es.clustername=es_flockdata
```

You can change this by passing in your own configuration settings, i.e.

```
-Des.clustername=mycluster
```


## Container Deployment
While this all looks rather technical, it is simply a matter of getting the WAR file deployed in a webserver. There is no "separate database" unless you wish to configure AB to talk to one.

If you're pressed for time and just want to see thing going, then you can run fd-search straight from the command line
```
$ cd fd-search/target
$ java -jar fd-search-0.91-BUILD-SNAPSHOT-war-exec.jar -Dfd.integration=http -httpPort=8081 -Dfd.config=./classes/config.properties -Dlog4j.configuration=file:./classes/log4j.xml
```

We assume you've read the all important [fd-engine](../fd-engine) container section. Here you can either run a separate TC instance, on port 8081 in this example, or you could deploy it in a seperate application context on the same TC instance.

## Interacting
There is a ping url

```
curl -X GET http://localhost:8081/api/v1/ping
```

And health of the service can be queried via a secured call

```
curl -u mike:123 -X GET http://localhost:8081/api/v1/health
```

Say hello to ElasticSearch
```
curl -X GET http://localhost:9201/_cluster/health?pretty=true
```

Generally that's it. There are some endpoints implemented using an @ServiceActivator pattern but these are reserved for fd-engine integration.

By default, fd-search spins up and ElasticSearch instance. It uses ports 9201 and 9301. This is basically so you can experiment with ElasticSearch clustering on one computer.

## Search Documents
FlockData indexes documents in a structure that follows [fd.{fortress-code}.{document-type}/{caller-ref}] structure. 

This lets you search wide or narrow index structures with wild cards. This approach keeps your syslog activity from logstash separate from your business domain data in the same cluster. Neat eh?

### Freetext Search
By integrating the "latest" version of data being changed in ElasticSearch, you have a powerful enterprise class way of searching all your computer systems for any data value. It's like a Google search for your domain data.

### Next steps for you!
Figure out how to query ElasticSearch. Deploy Kibana and start looking at your data in new ways.

