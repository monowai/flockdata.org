fd-search - ElasticSearch Facade
===============
This service is used by [fd-engine](../fd-engine) to talk to ElasticSearch or notionally any search product you would care to write a handler for. It listens for data integrating over http or amqp puts the docs into ElasticSearch

## Configuration
See [Spring](src/main/webapp/WEB-INF/spring) and [Application config](src/main/resources/config.properties) for various fd-search defaults. These can be overridden while fd-search is starting if you pass them in via the command line. But for evaluation purposes, they are good enough!

The ElasticSearch cluster that fd-search joins by default is 

```
es.clustername=es_flockdata
```

You can change this by passing in your own configuration settings, i.e.

```
-Des.clustername=mycluster
```
## Container Deployment

This is a SpringBoot application and honours all the standard SpringBoot configuration switches.

If you're pressed for time and just want to see thing going, then you can run fd-search straight from the command line
```
$ cd fd-search/target
$ java -jar fd-search-0.98.1.jar
```

### Security
fd-search does not have any security. All endpoints are unsecured. This is in contract to fd-engine which has a fully secured interface. It is not recommended that you expose fd-search or elasticsearch directly to the internet.


## Interacting


There is a ping url

```
curl -X GET http://localhost:8081/api/v1/admin/ping
```

And health of the service can be queried via a secured call

```
curl -X GET http://localhost:8081/api/v1/admin/health
```

Say hello to ElasticSearch
```
curl -X GET http://localhost:9200/_cluster/health?pretty=true
```

Generally that's it. There are some endpoints implemented using an @ServiceActivator pattern but these are reserved for fd-engine integration.

By default, fd-search spins up and ElasticSearch instance. It uses ports 9200 and 9300. This is basically so you can experiment with ElasticSearch clustering on one computer. You can run ElasticSearch standalone and have fd-search be a transport only client with a bit of configuration. See the application.yml file for further details

## Search Documents
FlockData indexes documents in a structure that follows [fd.{fortress-code}.{document-type}/{caller-ref}] structure. 

This lets you search wide or narrow index structures with wild cards. This approach keeps your syslog activity from logstash separate from your business domain data in the same cluster

### Freetext Search
By integrating the "latest" version of data being changed in ElasticSearch, you have a powerful enterprise class way of searching all your computer systems for any data value. It's like a Google search for your domain data.

### Next steps
Deploy [kibana](https://www.elastic.co/products/kibana) and start looking at your data in new ways.

