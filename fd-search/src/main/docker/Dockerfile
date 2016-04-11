FROM anapsix/alpine-java:latest

VOLUME /tmp

MAINTAINER Mike Holdsworth "https://github.com/monowai"

# docker run -p 8081:8081 -p 61001:61001 flockdata/fd-search

# Create flockdata var/lib folder
RUN mkdir /opt/flockdata && \
    mkdir /opt/flockdata/data && \
    mkdir /opt/flockdata/log && \
    mkdir /opt/flockdata/conf

WORKDIR /opt/flockdata

ADD fd-search-*.jar /opt/flockdata/search.jar

#ToDo: sort out where we pull logging configuration files from
ADD classes/log4j* /opt/flockdata/conf

# We don't need this. vars should be set in Compose. This will default to the properties stored within app.jar
COPY classes/application.yml /opt/flockdata/conf/application.yml


RUN bash -c 'touch /opt/flockdata/search.jar'
EXPOSE 8081 9201 9301 61001

ENTRYPOINT ["java",\
#     "-Xdebug", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=61001",\
     "-jar",\
     "/opt/flockdata/search.jar", \
     "-XX:-UseParNewGC -XX:-UseConcMarkSweepGC -XX:+UseG1GC -XX:+UseStringDeduplication", \
     "-Dlogging.config=/opt/flockdata/conf/log4j2.yml",\
     "-Djava.security.egd=file:/dev/./urandom"]

CMD -Xmx2g