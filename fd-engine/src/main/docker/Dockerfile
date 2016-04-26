FROM anapsix/alpine-java:latest

VOLUME /tmp

MAINTAINER Mike Holdsworth "https://github.com/monowai"

## Example ways to run the container, but you're probably using Vagrant or DockerCompose

# You'll get a stack trace if RabbitMQ isn't running:
# Start docker and expose the AMQP port
# docker run -p 5672:5672 rabbitmq:3-management

# Tell fd-engine where to find rabbit - the docker machine ip
# docker run -p 8080:8080 -p 61000:61000 -e rabbit.host=192.168.99.100 flockdata/fd-engine

## Get on to the command line
# docker run -it flockdata/fd-engine bash

# Create flockdata var/lib folder
RUN mkdir /opt/flockdata && \
    mkdir /opt/flockdata/data && \
    mkdir /opt/flockdata/log && \
    mkdir /opt/flockdata/conf

WORKDIR /opt/flockdata
# Add files.
COPY fd-engine-*.jar /opt/flockdata/engine.jar

COPY classes/application.yml /opt/flockdata
COPY classes/log4j* /opt/flockdata/conf
COPY classes/neo*.properties /opt/flockdata
COPY classes/application.yml /opt/flockdata/conf/application.yml

RUN bash -c 'touch /opt/flockdata/engine.jar'
ENV org.neo4j.path="/opt/flockdata"

# 8080 = Engine API 7474 is Neo4j Browser and 61000 for Java debugging
EXPOSE 8080 7474 61000

ENTRYPOINT ["java",\
    "-Dfile.encoding=UTF8", \
#    "-Xdebug", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=61000", \
     "-jar",\
     "/opt/flockdata/engine.jar", \
     "-XX:-UseParNewGC -XX:-UseConcMarkSweepGC -XX:+UseG1GC -XX:+UseStringDeduplication", \
     "-Dlogging.config=/opt/flockdata/conf/log4j2.yml",\
     "-Djava.security.egd=file:/dev/./urandom" \
     ]

CMD -Xmx2g