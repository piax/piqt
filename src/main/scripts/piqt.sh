#!/bin/sh
#
# Copyright (c) 2012-2015 Andrea Selva
# Copyright (c) 2016 PIAX development team
#

echo "  _____  __   ______  ______  "
echo "  |  _ \ | | /  ___ \ |_   _| "
echo "  | |_| || | | |   | |  | |   "
echo "  | ___/ | | | |   | |  | |   "
echo "  | |    | | | |___| |  | |   "
echo "  |_|    |_| \____/\_\  |_|   "
                                                                      
cd "$(dirname "$0")"

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# Only set PIQT_HOME if not already set
[ -f "$PIQT_HOME"/bin/piqt.sh ] || PIQT_HOME=`cd "$PRGDIR/.." ; pwd`
export PIQT_HOME

# Set JavaHome if it exists
if [ -f "${JAVA_HOME}/bin/java" ]; then 
   JAVA=${JAVA_HOME}/bin/java
else
   JAVA=java
fi
export JAVA

LOG_FILE=$PIQT_HOME/config/logging.properties
PIQT_PATH=$PIQT_HOME
JAVA_OPTS_SCRIPT="-XX:+HeapDumpOnOutOfMemoryError -Djava.awt.headless=true"

$JAVA $JAVA_OPTS $JAVA_OPTS_SCRIPT -Djava.util.logging.config.file="$LOG_FILE" -Duser.home="$PIQT_HOME" -Dpiqt.path="$PIQT_HOME" -cp "$PIQT_HOME/lib/*" org.piqt.web.Launcher $@

