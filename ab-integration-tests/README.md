Performs end-to-end integration tests between ab-engine and ab-search. Uses HTTP integration. You can only debug ab-engine.

ab-search is run as an embedded Tomcat controlled within the Maven POM.

Run the integration tests from this folder, with POM.xml, with the following command.

```
mvn clean install test -Dab.debug=false -Dab.integration=amqp -Dab.config=./src/test/resources/config.properties -P integration
```
