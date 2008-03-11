#!/bin/sh

#   Licensed to the Apache Software Foundation (ASF) under one
#   or more contributor license agreements.  See the NOTICE file
#   distributed with this work for additional information
#   regarding copyright ownership.  The ASF licenses this file
#   to you under the Apache License, Version 2.0 (the
#   "License"); you may not use this file except in compliance
#   with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing,
#   software distributed under the License is distributed on an
#   #  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#   KIND, either express or implied.  See the License for the
#   specific language governing permissions and limitations
#   under the License.

if [ "$UIMA_HOME" == "" ]
then
  echo UIMA_HOME environment variable is not set
  exit 1
fi

if [ "$1" = "" ];
then
  echo "Usage: startVinciService.sh svcdescriptor [vns_host]"
  exit
fi
. "$UIMA_HOME/bin/setUimaClassPath.sh"
if [ "$JAVA_HOME" = "" ]
then
  UIMA_JAVA_CALL=java
else
  UIMA_JAVA_CALL="$JAVA_HOME/bin/java"
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

"$UIMA_JAVA_CALL" -cp "$UIMA_CLASSPATH" "-Duima.datapath=$UIMA_DATAPATH" -DVNS_HOST=$VNS_HOST -DVNS_PORT=$VNS_PORT "-Djava.util.logging.config.file=$UIMA_LOGGER_CONFIG_FILE" $UIMA_JVM_OPTS org.apache.uima.adapter.vinci.VinciAnalysisEngineService_impl $SERVICE $INSTANCEID


