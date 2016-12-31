fd-engine  - Information Tracking Service and API
===========
FlockData's meta-data engine service. This service coordinates data updates to KV Stores and Neo4J to support tracking and exploration of data.

Interactions between a client and FD occur via fd-engine. fd-engine writes "the latest" change of your information in to [fd-search](../fd-search). Is is also possible to write via fd-engine directly to ElasticSearch and bypass the link exploration if your dataset is not highly connected;  Some statistical dumps can be like this.

fd-engine runs embedded Neo4J and exposes the REST api on port 7474 so exploration tools will work with fd-engine just as they do with Neo4j stand-alone. You can also interact with the database via `neo4j-shell` which is part of the standard distribution of Neo4J.

The concepts and calls are explained in the [Flock Service Call Wiki](http://www.monowai.com/wiki/pages/viewpage.action?pageId=13172790)

Please see our [PostMan API gist](../fd.api-postman.json)  for a quick and convenient way of making REST calls to FlockData.

## Dependencies
Only mandatory dependency at this time is RABBIT MQ for integration - all defaults work out of the box; This should be installed separately according to your OS instructions.

## Source based installation
Get the source
```
$ git clone https://github.com/monowai/flockdata.org
```

Build with dependencies, including running the tests
```
$ mvn install
```

## Configuration
fd-engine exchanges information with fd-search via http and amqp integration.

### Authorisation
In this version of FlockData security configuration is controlled by activating a security profile. We currently support the following
* `fd-auth-test` - Allows you to set simple user/password

We have work underway to support Stormpath as a security provider but this is currently using outdated libraries and is not supported at this stage.

If you don't supply any users to the configuration, then the default of `mike/123` is used.

Passing these arguments to FlockData when starint will create a user called `myuser` with a password of 123 will be authorised to create DataAccessUsers in FlockData.
    `--org.fd.auth.simple.users.myuser.pass=123 --org.fd.auth.simple.users.myuser.roles="FD_USER;FD_ADMIN"`

Users can also be set in your `application.yml` file


## Start Your Engines
fd-engine runs as a SpringBoot application. This example assumes you have completed a `mvn package` once so that the app is actually built.

```
cd fd-engine/target
java -jar fd-engine-0.98.1.jar --org.neo4j.path=classes --org.fd.auth.simple.users.myuser.pass=123 --org.fd.auth.simple.users.myuser.roles="FD_USER;FD_ADMIN"
```

Default HTTP port for fd-engine is 8080 and for fd-search its 8081. If you are using different ports then review the [configuration files] (src/main/resources/application.yml) that describe how engine and search find each other. If you have an existing ElasticSearch cluster, you will also want to review

## Interacting with FlockData
HTTP, REST and JSON is the lingua franca.

### Authenticate and see who you are
```
curl -u myuser:123 -X GET http://localhost:8080/api/account/
```
An authorised user is one with one of the two FD_ auth roles. And FD_ADMIN user does not automatically inherit the rights to read and write data, but they can create users who can do this.

### Register yourself as a data access user
```
curl -u myuser:123 -H "Content-Type:application/json" -X POST http://localhost:8080/api/v1/profiles/ -d '{"name":"myuser", "companyName":"Monowai","login":"myuser"}'
```
This command connects your authenticated user to a FlockData SystemUser. A company called monowai is created that will own the data being tracked through the service.

## Tracking Data
By default, information is tracked in Neo4J and ElasticSearch. You can, at the point of POST, request that the information be only tracked in Neo4j or only ElasticSearch. This depends on your use case. You might be simply tracking event type information that never changes, so simply storing in ElasticSearch is functional enough as the data is not require the meshing of connections.

### Create an Application Fortress
This is one of your computer systems that you want to track information coming from
```
curl -u myuser:123 -H "Content-Type:application/json" -X POST http://localhost:8080/api/v1/fortress/ -d '{"name": "dataService","searchEnabled": true}'
-- Have a look at the fortress you just created
curl -u myuser:123 -X GET http://localhost:8080/api/v1/fortress/
```
### Track a Data Event
Make sure you have started [fd-search](../fd-search)
```
curl -u myuser:123 -H "Content-Type:application/json" -X POST http://localhost:8080/api/v1/track/ -d '{
  "fortress": {"name":"dataService"},
  "type": {"name":"Debtor"},
  "code":"myPrimaryKey",
  "log": { "data": {"BusinessData": "Your Text", "nestedObject": {"someData": "kool for kats"}}
  }
}'

-- If you take note of the key returned by the previous call you can then execute this call:
curl -umyuser:123 -X GET http://localhost:8080/api/v1/entity/LykuJI_TTVG2nW33WVy6qA
-- List all logs associated wtih the entity
curl -umyuser:123 -X GET http://localhost:8080/api/v1/entity/LykuJI_TTVG2nW33WVy6qA/log
-- retreive the data stored with the "last" log
curl -umyuser:123 -X GET http://localhost:8080/api/v1/entity/LykuJI_TTVG2nW33WVy6qA/log/last/data

```
### Find that doc in ElasticSearch
Note that fd-search is configured by default to have ElasticSearch listening on port 9200. This command is an ElasticSearch query, not an fd-search one.

```
curl -H "Content-Type:application/json" -X POST http://localhost:9200/fd.*/_search -d '{
    query: {
          query_string : {
            "query" : "kool"
           }
      }
}'
````

A proxy query can via fd-engine can also be run. FD will ensure that the authenticated user is only returned data for the Company and Fortresses that they are authorised to view:
This query returns a sub-set of data that is useful to display in search results, including where the result was found.

```
curl -u myuser:123 -X POST  -H "Content-Type: application/json" -d '{

  "searchText": "kool",
  "from": 0,
  "size":6

}
' "http://localhost:8080/api/v1/query/"
```
Raw ElasticSearch queries can be passed through the `query/es` endpoint. This provides secure access to data while giving you
full access to the expressive ElasticSearch DSL.

```
curl -u myuser:123 -X POST -H "Content-Type: application/json" -d '{
  "query": {
    "filtered": {
      "query": {
        "query_string": {
          "query": "kool"
        }
      }
    }
  }
}' "http://localhost:8080/api/v1/query/es"
```

### Next steps....
Track one of your entities to the service via [API](http://www.monowai.com/wiki/pages/viewpage.action?pageId=13172790) documentation. Understand and add [Tags](http://www.monowai.com/wiki/pages/viewpage.action?pageId=13172831). Process a batch of changes. Log a single change to an existing entity.
