ab-common - Core objects
===============

This library contains Model interfaces and common helpers. From here you can implement the interfaces to provide support for a data storage mechanism of your own choice. 

Of particular note in here is [Helper](src/main/java/com/auditbucket/helper/) package. It includes AbExporter which is a Java client that let's you interact with AB from within your own application.

[ab-spring}(../ab-spring) uses this to talk to AB.
