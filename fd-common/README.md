fd-common - Core objects
===============

This library contains Model interfaces and common helpers. From here you can implement the interfaces to provide support for a data storage mechanism of your own choice. 

Of particular note in here is [Helper](src/main/java/org/flockdata/helper/) package. It includes FdRestClient which is a Java client that let's you interact with FD from within your own application.

[fd-spring}(../fd-spring) uses this to talk to FlockData.
