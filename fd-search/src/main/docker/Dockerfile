FROM anapsix/alpine-java:latest

VOLUME /tmp

MAINTAINER Mike Holdsworth "https://github.com/monowai"

# Create flockdata var/lib folder
RUN mkdir /opt/flockdata && \
    mkdir /opt/flockdata/data && \
    mkdir /opt/flockdata/log && \
    mkdir /opt/flockdata/bin && \
    mkdir /opt/flockdata/conf

ADD fd-search-*.jar /opt/flockdata/search.jar

#ToDo: sort out where we pull logging configuration files from
ADD classes/log4j* /opt/flockdata/conf

# We don't need this. vars should be set in Compose. This will default to the properties stored within app.jar
COPY classes/application.yml /opt/flockdata/conf/application.yml


RUN bash -c 'touch search.jar'
EXPOSE 8081 9201 9301
ENV JAVA_OPTS="-Xmx2g"
ENTRYPOINT ["java","-jar","/opt/flockdata/search.jar", "-Dlogging.config=/opt/flockdata/conf/log4j2.yml","-Djava.security.egd=file:/dev/./urandom","${JAVA_OPTS}"]