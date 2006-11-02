#!/bin/sh
if [ "$JAVA_HOME" = "" ];
then
  JAVA_HOME=$UIMA_HOME/java/jre
fi
"$JAVA_HOME/bin/java" -cp "$UIMA_HOME/lib/vinci/jVinci.jar" org.apache.vinci.transport.vns.service.VNS $*


