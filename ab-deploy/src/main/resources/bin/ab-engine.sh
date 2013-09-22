#!/bin/sh
FRIENDLY_NAME="AuditBucket Engine Server"
LAUNCHD_NAME="com.auditbucket.engine"

function findBaseDirAndCdThere {
    # This seems to not be safe to run at any time. If that
    # is the case, it should be fixed to be so, if possible.
    CDPATH=""
    SCRIPT="$0"

    # SCRIPT may be an arbitrarily deep series of symlinks. Loop until we have the concrete path.
    while [ -h "$SCRIPT" ] ; do
      ls=`ls -ld "$SCRIPT"`
      # Drop everything prior to ->
      link=`expr "$ls" : '.*-> \(.*\)$'`
      if expr "$link" : '/.*' > /dev/null; then
        SCRIPT="$link"
      else
        SCRIPT=`dirname "$SCRIPT"`/"$link"
      fi
    done

  AB_HOME=`cd $( dirname "$SCRIPT" )/.. && dirs -l +0`
  AB_INSTANCE=$AB_HOME
  AB_CONFIG=$AB_INSTANCE/conf
  AB_APP=$AB_INSTANCE/app
  AB_LOG=$AB_INSTANCE/data/log

  cd "$AB_HOME"
}

function parseConfig {
  if [ ${BASH_VERSINFO[0]} -eq 3 ] ; then
    if [ ${BASH_VERSINFO[1]} -lt 2 ] ; then
      getconfigquoted "${AB_CONFIG}/abengine.properties"
      return
    fi
  fi
  getconfig "${AB_CONFIG}/abengine.properties"
}

buildclasspath() {
  # confirm library jars
  if [ ! -e "$AB_APP" ] ; then
    echo "Error: missing AB-Engine Application, expected at $AB_APP"
    exit 1
  fi

  CLASSPATH=${ALL_JARS}

  # add useful conf stuff to classpath - always a good idea
  CLASSPATH="$CLASSPATH":"$AB_HOME"/conf/
}

showinfo() {
  reportstatus
  buildclasspath

  echo "ABE_HOME:        $AB_HOME"
  echo "ABE_INTEGRATION: $AB_INTEGRATION"
  echo "ABE_NEO4J:       $ABE_NEO4J"
  echo "ABE_HTTP_PORT:   $ABE_HTTP_PORT"
  echo "ABE_INSTANCE:    $AB_INSTANCE"
  echo "JAVA_HOME:       $JAVA_HOME"
  echo "JAVA_OPTS:       $JAVA_OPTS"
  echo "CLASSPATH:       $CLASSPATH"
}


if [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA=$(which java)
fi

if [ ! -x "$JAVA" ]; then
    echo "Could not find any executable java binary. Please install java in your PATH or set JAVA_HOME"
    exit 1
fi
findBaseDirAndCdThere
buildclasspath
source bin/utils
parseConfig

ABE_HTTP_PORT="${wrapper_httpPort:=9092}"
AB_INTEGRATION="${wrapper_ab_integration:=http}"
ABE_NEO4J=${wrapper_neo4j}

JAVA_OPTS="headless=true -XX:+DisableExplicitGC ${wrapper_java_additional} --debug"

[ -z "${wrapper_java_initmemory}" ] || JAVA_OPTS="$JAVA_OPTS -Xms${wrapper_java_initmemory}m"
[ -z "${wrapper_java_maxmemory}" ] || JAVA_OPTS="$JAVA_OPTS -Xmx${wrapper_java_maxmemory}m"

showinfo

$JAVA -cp '$CLASSPATH' -jar $AB_APP/ab-engine-1.0.0-BUILD-SNAPSHOT-war-exec.jar $JAVA_OPTS  \
        -Djava.awt.headless=true \
        -Dneo4j=${ABE_NEO4J} \
        -httpPort=${ABE_HTTP_PORT} \
        -Dab.integration=${AB_INTEGRATION}  \
        -Dab.config=${AB_CONFIG}/config.properties  \
        -extractDirectory=".extracts/engine"\
        -DAB_HOME=${AB_HOME} \
        -Dlog4j.configuration=file://${AB_CONFIG}/log4j-engine.xml
        #-Djava.util.logging.config.file=file://${AB_CONFIG}/logging.properties



#java -jar ab-engine-1.0.0-BUILD-SNAPSHOT-war-exec.jar -Dab.integration=http -httpPort=8090 -Dlog4j.configuration=./log4j.xml -Dab.config=./config.properties
