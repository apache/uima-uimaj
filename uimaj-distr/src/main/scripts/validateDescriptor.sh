#!/bin/sh
. "$UIMA_HOME/bin/setUimaClassPath.sh"
if [ "$JAVA_HOME" = "" ];
then
  JAVA_HOME=$UIMA_HOME/java/jre
fi
"$JAVA_HOME/bin/java" -cp "$UIMA_CLASSPATH" "-Duima.datapath=$UIMA_DATAPATH" -Xms128M -Xmx256M org.apache.uima.tools.ValidateDescriptor $*
