FROM java:8
VOLUME /tmp

# Create flockdata var/lib folder
RUN mkdir /opt/flockdata && \
    mkdir /opt/flockdata/app && \
    mkdir /opt/flockdata/data && \
    mkdir /opt/flockdata/log && \
    mkdir /opt/flockdata/conf

# Add files.
COPY fd-engine-war.jar /opt/flockdata/app/app.jar
COPY classes/log4j.xml /opt/flockdata/conf/log4j.xml
# We don't need this. vars should be set in Compose. This will default to the properties stored within app.jar
#COPY classes/application.properties /opt/flockdata/conf/application.properties

RUN bash -c 'touch app.jar'
EXPOSE 8080 7474
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/opt/flockdata/app/app.jar","-httpPort=8080"]