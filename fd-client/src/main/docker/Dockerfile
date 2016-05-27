FROM anapsix/alpine-java:latest

VOLUME /tmp

MAINTAINER Mike Holdsworth "https://github.com/monowai"


## Get on to the command line
# docker run -it flockdata/fd-client bash

# Create flockdata var/lib folder
RUN mkdir /opt/flockdata && \
    mkdir /opt/flockdata/bin && \
    mkdir /opt/flockdata/log && \
    mkdir /opt/flockdata/data && \
    mkdir /opt/flockdata/model && \
    mkdir /opt/flockdata/conf

WORKDIR /opt/flockdata
# Add files.
COPY fd-client*.jar /opt/flockdata/fd-client.jar
COPY classes/*.json /opt/flockdata/model/
COPY classes/fd-cow.txt /opt/flockdata/data/
COPY classes/states.csv /opt/flockdata/data/
COPY classes/application.yml /opt/flockdata/
COPY classes/log4j* /opt/flockdata/conf/
COPY classes/application.yml /opt/flockdata/conf/application.yml

# Command scripts
COPY classes/commands/ /opt/flockdata/bin/

ENV FD_BASE="/opt/flockdata"
RUN ln -s $FD_BASE/bin/fdimport /usr/local/bin/fdimport
RUN ln -s $FD_BASE/bin/fdcountries /usr/local/bin/fdcountries
RUN ln -s $FD_BASE/bin/fdregister /usr/local/bin/fdregister
RUN ln -s $FD_BASE/bin/fdutils /usr/local/bin/fdutils

RUN alias ll='ls -la'

## Useful for running API commands
#RUN apk -U add curl

RUN bash -c 'touch /opt/flockdata/fd-client.jar'
RUN chmod +x  /opt/flockdata/bin/*

CMD bash

