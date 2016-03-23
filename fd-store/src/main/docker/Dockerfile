FROM anapsix/alpine-java:latest

VOLUME /tmp

MAINTAINER Mike Holdsworth "https://github.com/monowai"

# Create flockdata var/lib folder
RUN mkdir /opt/flockdata && \
    mkdir /opt/flockdata/app && \
    mkdir /opt/flockdata/data && \
    mkdir /opt/flockdata/log && \
    mkdir /opt/flockdata/conf

# Add files.
COPY fd-store-*.jar /opt/flockdata/store.jar
COPY classes/log4j* /opt/flockdata/conf
COPY classes/application.yml /opt/flockdata/
# We don't need this. vars should be set in Compose. This will default to the properties stored within app.jar
#COPY classes/application.yml /opt/flockdata/conf/application.yml

RUN bash -c 'touch store.jar'
ENV JAVA_OPTS="-Xmx1g"
EXPOSE 8082
ENTRYPOINT ["java","-jar","/opt/flockdata/store.jar","-Dlogging.config=/opt/flockdata/conf/log4j2.yml","-Djava.security.egd=file:/dev/./urandom","${JAVA_OPTS}"]