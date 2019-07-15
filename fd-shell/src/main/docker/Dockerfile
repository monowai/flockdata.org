FROM anapsix/alpine-java:latest

VOLUME /tmp

MAINTAINER Mike Holdsworth "https://github.com/monowai"


## Get on to the command line
# docker run -it flockdata/fd-client bash

# Create flockdata var/lib folder
RUN mkdir /opt/flockdata && \
    mkdir /opt/flockdata/log && \
    mkdir /opt/flockdata/data && \
    mkdir /opt/flockdata/model && \
    mkdir /opt/flockdata/conf

WORKDIR /opt/flockdata
# Add files.
COPY classes/application.yml ./application.yml
COPY classes/countries.json /opt/flockdata/model/.
COPY classes/fd-cow.txt /opt/flockdata/data/.
COPY classes/states.json /opt/flockdata/model/.
COPY classes/states.csv /opt/flockdata/data/.

COPY fd-shell-*.jar /opt/flockdata/fd-shell.jar

ENV FD_BASE="/opt/flockdata"

RUN alias ll='ls -la'

## Useful for running API commands
#RUN apk -U add curl

RUN bash -c 'touch /opt/flockdata/fd-shell.jar'
#RUN chmod +x  /opt/flockdata/bin/*

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dfile.encoding=UTF8", "-jar","/opt/flockdata/fd-shell.jar"]
