#!/bin/sh
. setUimaClassPath.sh
if [ "$JAVA_HOME" = "" ];
then
  JAVA_HOME=$UIMA_HOME/java/jre
fi
"$JAVA_HOME/bin/java" -cp "$UIMA_CLASSPATH" "-Duima.datapath=$UIMA_DATAPATH" -DVNS_HOST=$VNS_HOST -DVNS_PORT=$VNS_PORT "-Djava.util.logging.config.file=$UIMA_HOME/Logger.properties" -Xms128M -Xmx256M org.apache.uima.examples.cpe.SimpleRunCPE $*
