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
COPY classes/log4j* /opt/flockdata/conf/
COPY classes/application.yml /opt/flockdata/conf/application.yml

COPY fd-shell-*.jar /opt/flockdata/fd-shell.jar
#COPY ../../fd-client/*.json /opt/flockdata/model/
#COPY ../../fd-client/fd-cow.txt /opt/flockdata/data/
#COPY ../../fd-client/states.csv /opt/flockdata/data/
#COPY ../../fd-client/application.yml /opt/flockdata/

ENV FD_BASE="/opt/flockdata"
#RUN ln -s $FD_BASE/bin/fdshell /usr/local/bin/fdshell
#RUN ln -s $FD_BASE/bin/fdutils /usr/local/bin/fdutils

RUN alias ll='ls -la'

## Useful for running API commands
#RUN apk -U add curl

RUN bash -c 'touch /opt/flockdata/fd-shell.jar'
#RUN chmod +x  /opt/flockdata/bin/*

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dfile.encoding=UTF8", "-jar","/opt/flockdata/fd-shell.jar", "--profiles","fd-client"]
