FROM java:8
VOLUME /tmp

# Create flockdata var/lib folder
RUN mkdir /opt/flockdata && \
    mkdir /opt/flockdata/data && \
    mkdir /opt/flockdata/log && \
    mkdir /opt/flockdata/bin && \
    mkdir /opt/flockdata/conf

# Add files.
ADD fd-search-*.jar /opt/flockdata/bin/app.jar
ADD classes/log4j* /opt/flockdata/conf
# We don't need this. vars should be set in Compose. This will default to the properties stored within app.jar
COPY classes/application.yml /opt/flockdata/conf/application.yml


RUN bash -c 'touch app.jar'
EXPOSE 8081
#"-Dfd.config=/opt/flockdata/conf/config.properties","-Dlog4j.configuration=file:/opt/flockdata/conf/log4j.xml"
ENTRYPOINT ["java","-Dlogging.config=/opt/flockdata/conf/log4j2.yml","-Djava.security.egd=file:/dev/./urandom","-jar","/opt/flockdata/bin/app.jar","-httpPort=8081"]