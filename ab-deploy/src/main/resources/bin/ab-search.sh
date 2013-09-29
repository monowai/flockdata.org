#!/bin/bash

LAUNCHD_NAME="com.auditbucket.search"
FRIENDLY_NAME="AuditBucket Search Service"
TIMEOUT=120

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
    if [ ! -d "$AB_HOME/data" ]; then
      mkdir "$AB_HOME/data"
  fi
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
  buildclasspath
  detectrunning
  if [ $newpid ] ; then
      echo "$FRIENDLY_NAME is running on $newpid"
   else
      echo "$FRIENDLY_NAME is not running"
  fi

  echo "ABS_HOME:        $AB_HOME"
  echo "ABS_HTTP_PORT:   $ABS_HTTP_PORT"
  echo "ABS_INTEGRATION: $AB_INTEGRATION"
  echo "ABS_INSTANCE:    $AB_INSTANCE"
  echo "JAVA_HOME:       $JAVA_HOME"
  echo "JAVA_OPTS:       $JAVA_OPTS"
  echo "CLASSPATH:       $CLASSPATH"
}

checkstatus() {

  if [ -e "$PID_FILE" ] ; then
    ABS_PID=$( cat "$PID_FILE" )
    kill -0 $ABS_PID 2>/dev/null || ABE_PID=
  fi
}

detectrunning() {
  ## This could be achieved with filtering using -sTCP:LISTEN but this option is not available
  ## on lsof v4.78 which is the one bundled with some distros. So we have to do this grep below
  newpid=$(lsof -i :$ABS_HTTP_PORT -F T -Ts | grep -i "TST=LISTEN" -B1 | head -n1)
  newpid=${newpid:1}
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
PID_FILE=$AB_HOME/data/abs.pid

JAVA_OPTS="headless=true -XX:+DisableExplicitGC ${wrapper_java_additional}"

[ -z "${wrapper_java_initmemory}" ] || JAVA_OPTS="$JAVA_OPTS -Xms${wrapper_java_initmemory}m"
[ -z "${wrapper_java_maxmemory}" ] || JAVA_OPTS="$JAVA_OPTS -Xmx${wrapper_java_maxmemory}m"


startit() {
    echo "looking to start"
    detectos
    #showinfo
     if [ $DIST_OS = "macosx" ] ; then
        getlaunchdpid
        if [ $LAUNCHDPID -eq 0 ] ; then
          echo "Detected installation in launchd, starting it..."
          launchctl start $LAUNCHD_NAME
          exit 0
        elif [ $LAUNCHDPID -gt 0 ] ; then
          echo "Instance already running via launchd with PID $LAUNCHDPID"
          exit 1
        fi
        # We fall through here since if there is no launchd install we start manually
      fi

        detectrunning
        if [ $newpid ] ; then
	         echo "Another server-process is running with [$newpid], cannot start a new one. Exiting."
	        exit 2;
        fi

      checkstatus

      if [ -z $ABS_PID ] ; then
        printf "Starting $FRIENDLY_NAME..."

        if [ -e "$PID_FILE" ] ; then
            echo "removing stale PID file"
            rm $PID_FILE
        fi

        $JAVA -cp '$CLASSPATH' -jar $AB_APP/ab-search-1.0.0-BUILD-SNAPSHOT-war-exec.jar $JAVA_OPTS  \
            -Djava.awt.headless=true \
            -httpPort=${ABS_HTTP_PORT} \
            -Dab.integration=${AB_INTEGRATION} \
            -Dab.config=${AB_CONFIG}/config.properties \
            -extractDirectory=".extracts/search" \
            -DAB_HOME=${AB_HOME} \
            -Dlog4j.configuration=file://${AB_CONFIG}/log4j-search.xml  & > "$AB_HOME/log/abs-console.txt" & ps aux |grep [h]ttpPort=${ABE_HTTP_PORT} | awk '/[a]b-search/{print $2}' >> $PID_FILE

        disown

      else
          echo "$FRIENDLY_NAME already running with pid $ABE_PID"
          exit 0
      fi
}

stopit() {

  detectos
  checkwriteaccess

  if [ $DIST_OS = "macosx" ] ; then
    getlaunchdpid
    if [ $LAUNCHDPID -gt 0 ] ; then
      echo "Instance running via launchd with PID $LAUNCHDPID, stopping it..."
      launchctl stop $LAUNCHD_NAME
      return
    fi
    # We fall through here since if there is no launchd install we stop manually started instance
  fi

  ABS_PID=$( cat "$PID_FILE" )

  if [ -z $ABS_PID ] ; then
    echo "ERROR: $FRIENDLY_NAME not running"
    [ -e "$PID_FILE" ] && rm "$PID_FILE"
  else
    printf "Stopping $FRIENDLY_NAME [$ABS_PID]..."
    x=0
    while [ $x -lt $TIMEOUT ] && [ "$ABS_PID" != "" ]  ; do
      kill $ABS_PID 2>/dev/null
      printf "."
      sleep 1
      detectrunning
      if [ !"$newpid" ] ; then
        echo "Stopped"
        break
      fi
      x=$[$x+1]
    done
	  [  $x -eq $TIMEOUT ] && ( echo " force shutdown" ;  kill -9 $ABE_PID>/dev/null ) || echo " done"
	  [ -e "$PID_FILE" ] && rm  "$PID_FILE"
  fi
}


case "${!OPTIND}" in
  console)
    console
    exit 0
    ;;

  start)
    WAIT=true
    startit
    ;;

  start-no-wait)
    WAIT=false
    startit
    exit 0
    ;;

  stop)
    stopit
    exit 0
    ;;

  restart)
    WAIT=true
    stopit
    startit
    exit 0
    ;;

  status)
    reportstatus
    exit 0
    ;;

  info)
    showinfo
    exit 0
    ;;
  install)
    WAIT=true
    installservice
    exit 0
    ;;
  remove)
    removeservice
    exit 0
    ;;
  *)
    echo "Usage: ab-engine { console | start | start-no-wait | stop | restart | status | info | install | remove }"
    exit 0;;
esac