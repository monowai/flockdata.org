#!/bin/bash

LAUNCHD_NAME="com.auditbucket.search"
FRIENDLY_NAME="AuditBucket Search Server"
function findBaseDirAndCdThere {
# This seems to not be safe to run at any time. If that
# is the case, it should be fixed to be so, if possible.
  SCRIPT=$0

  cd "`dirname "$SCRIPT"`"
  SCRIPT=`basename "$SCRIPT"`

  while [ -L "$SCRIPT" ]
  do
    SCRIPT=$( readlink "$SCRIPT" )
    cd "$(dirname "$SCRIPT")"
    SCRIPT=`basename "$SCRIPT"`
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
      getconfigquoted "${AB_CONFIG}/absearch.properties"
      return
    fi
  fi
  getconfig "${AB_CONFIG}/absearch.properties"
}

buildclasspath() {
  # confirm library jars
  if [ ! -e "$AB_APP" ] ; then
    echo "Error: missing AB-Search Application, expected at $AB_APP"
    exit 1
  fi

  # CLASSPATH=${ALL_JARS}

  # add useful conf stuff to classpath - always a good idea
  CLASSPATH="$CLASSPATH":"$AB_HOME"/conf/
}

showinfo() {
  reportstatus
  buildclasspath

  echo "ABS_HOME:        $AB_HOME"
  echo "ABS_HTTP_PORT:   $ABS_HTTP_PORT"
  echo "ABS_INTEGRATION: $AB_INTEGRATION"
  echo "ABS_INSTANCE:    $AB_INSTANCE"
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

ABS_HTTP_PORT=${wrapper_httpPort:=9092}
AB_INTEGRATION=${wrapper_ab_integration:=http}
JAVA_OPTS="headless=true -XX:+DisableExplicitGC ${wrapper_java_additional}"

[ -z "${wrapper_java_initmemory}" ] || JAVA_OPTS="$JAVA_OPTS -Xms${wrapper_java_initmemory}m"
[ -z "${wrapper_java_maxmemory}" ] || JAVA_OPTS="$JAVA_OPTS -Xmx${wrapper_java_maxmemory}m"

showinfo

 $JAVA -cp '$CLASSPATH' -jar $AB_APP/ab-search-1.0.0-BUILD-SNAPSHOT-war-exec.jar $JAVA_OPTS  \
        -Djava.awt.headless=true \
        -httpPort=${ABS_HTTP_PORT} \
        -Dab.integration=${AB_INTEGRATION} \
        -Dab.config=${AB_CONFIG}/config.properties \
        -extractDirectory=".extracts/search" \
        -DAB_HOME=${AB_HOME} \
        -Dlog4j.configuration=file://${AB_CONFIG}/log4j-search.xml
        #-Djava.util.logging.config.file=file://${AB_CONFIG}/logging.properties


#        -Dlog4j.configuration=\"${ABS_CONFIG}/log4j.xml\" \
#        -Djava.util.logging.config.file=\"${ABS_CONFIG}/logging.properties\" \


#java -jar ab-engine-1.0.0-BUILD-SNAPSHOT-war-exec.jar -Dab.integration=http -httpPort=8090 -Dlog4j.configuration=./log4j.xml -Dab.config=./config.properties
