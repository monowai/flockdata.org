FROM anapsix/alpine-java:latest

VOLUME /tmp

MAINTAINER Mike Holdsworth "https://github.com/monowai"

## Example ways to run the container, but you're probably using Vagrant or DockerCompose
# docker run -p 8082:8082 -p 61002:61002 flockdata/fd-store
## Get on to the command line
# docker run -it flockdata/fd-store bash

# Create flockdata var/lib folder
RUN mkdir /opt/flockdata && \
    mkdir /opt/flockdata/data && \
    mkdir /opt/flockdata/log && \
    mkdir /opt/flockdata/conf

WORKDIR /opt/flockdata
# Add files.
COPY fd-store-*.jar /opt/flockdata/store.jar

COPY classes/application.yml /opt/flockdata
COPY classes/log4j* /opt/flockdata/conf
COPY classes/application.yml /opt/flockdata/conf/application.yml

RUN bash -c 'touch /opt/flockdata/store.jar'

# 8082 = store API 61002 for Java debugging
EXPOSE 8082 61002

ENTRYPOINT ["java",\
      "-Dfile.encoding=UTF8", \
#     "-Xdebug", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=61002",\
     "-jar",\
     "/opt/flockdata/store.jar", \
     "-XX:-UseParNewGC -XX:-UseConcMarkSweepGC -XX:+UseG1GC -XX:+UseStringDeduplication", \
     "-Dlogging.config=/opt/flockdata/conf/log4j2.yml",\
     "-Djava.security.egd=file:/dev/./urandom"]

CMD -Xmx1g