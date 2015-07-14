Performs end-to-end integration tests between fd-engine and fd-search. Uses HTTP integration. 

See the comments in TestFdIntegration for tips on debugging and creating tests

fd-search is run as an embedded Tomcat controlled within the Maven POM.

Run the integration tests from this folder, with POM.xml, with the following command.

```
mvn clean install test -Dfd.debug=false -P integration
```
