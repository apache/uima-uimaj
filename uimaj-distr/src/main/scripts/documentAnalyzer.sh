#!/bin/sh
. "$UIMA_HOME/bin/setUimaClassPath.sh"
if [ "$JAVA_HOME" = "" ];
then
  JAVA_HOME=$UIMA_HOME/java/jre
fi
"$JAVA_HOME/bin/java" -cp "$UIMA_CLASSPATH" -Xms128M -Xmx900M "-Duima.home=$UIMA_HOME" "-Duima.datapath=$UIMA_DATAPATH" -DVNS_HOST=$VNS_HOST -DVNS_PORT=$VNS_PORT "-Djava.util.logging.config.file=$UIMA_HOME/Logger.properties" org.apache.uima.tools.docanalyzer.DocumentAnalyzer
