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
COPY fd-store-*.jar /opt/flockdata/app/app.jar
COPY classes/log4j* /opt/flockdata/conf
COPY classes/application.yml /opt/flockdata/
# We don't need this. vars should be set in Compose. This will default to the properties stored within app.jar
#COPY classes/application.yml /opt/flockdata/conf/application.yml

RUN bash -c 'touch app.jar'
EXPOSE 8080 7474
ENTRYPOINT ["java","-Dlogging.config=/opt/flockdata/conf/log4j2.yml","-Djava.security.egd=file:/dev/./urandom","-jar","/opt/flockdata/app/app.jar"]