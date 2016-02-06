FROM java:8
VOLUME /tmp

# Create flockdata var/lib folder
RUN mkdir /var/lib/flockdata && \
    mkdir /var/lib/flockdata/app && \
    mkdir /var/lib/flockdata/data && \
    mkdir /var/lib/flockdata/log && \
    mkdir /var/lib/flockdata/bin && \
    mkdir /var/lib/flockdata/conf

# Add files.
ADD fd-engine-war.jar /var/lib/flockdata/bin/app.jar
ADD classes/config.properties /var/lib/flockdata/conf/config.properties
ADD classes/log4j.xml /var/lib/flockdata/conf/log4j.xml

RUN bash -c 'touch /app.jar'
EXPOSE 8080 7474
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/var/lib/flockdata/bin/app.jar","-httpPort=8080","-Dneo4=java","-Dfd.integration=http","-Dfd.auth.provider=simple","-Dfd.config=/var/lib/flockdata/conf/config.properties","-Dlog4j.configuration=file:/var/lib/flockdata/conf/log4j.xml"]