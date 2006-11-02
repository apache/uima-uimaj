#!/bin/sh
if [ "$1" = "" ];
then
  echo "Usage: startVinciService.sh svcdescriptor [vns_host]"
  exit
fi
. "$UIMA_HOME/bin/setUimaClassPath.sh"
if [ "$JAVA_HOME" = "" ];
then
  JAVA_HOME=$UIMA_HOME/java/jre
fi

SERVICE=$1

INSTANCEID=0

if [ "$2" != "" ] && [ "$3" != "" ]
then
   INSTANCEID=$3
fi  

if [ "$2" != "" ];
then
  VNS_HOST=$2
fi

"$JAVA_HOME/bin/java" -cp "$UIMA_CLASSPATH" "-Duima.datapath=$UIMA_DATAPATH" -DVNS_HOST=$VNS_HOST -DVNS_PORT=$VNS_PORT "-Djava.util.logging.config.file=$UIMA_HOME/Logger.properties" org.apache.uima.adapter.vinci.VinciCasObjectProcessorService_impl $SERVICE $INSTANCEID


