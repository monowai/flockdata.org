fd-discovery
========

Experimental package for standing up [spring-cloud-netflix])(http://cloud.spring.io/spring-cloud-netflix/) integration

If you want to run this then when starting the fd-services, you should activate the profile `discovery`

See the classes `DiscoveryConfig.java` for further clues.

Note that on Docker-Compose running on non-linux based machines, the HOST IP is not translated properly by the Eureka service