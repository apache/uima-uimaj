#!/bin/sh -vx

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

if [ $# -ge 1 ]
then
  firstarg=$1
fi
if [ $# -ge 2 ]
then
  secondarg=$2
fi
. "$UIMA_HOME/bin/setUimaClassPath.sh"
echo "Running JCasGen with no Java CAS Model merging.  To run with merging, use jcasgen_merge (requires Eclipse, plus UIMA and EMF plugins)."
if [ "$JAVA_HOME" = "" ]
then
  UIMA_JAVA_CALL=java
else
  UIMA_JAVA_CALL="$JAVA_HOME/bin/java"
fi
if [ "$UIMA_LOGGER_CONFIG_FILE" = "" ]
then
  UIMA_LOGGER_CONFIG_FILE=$UIMA_HOME/config/Logger.properties
fi
LOGGER="-Djava.util.logging.config.file=$UIMA_LOGGER_CONFIG_FILE"
MAIN=org.apache.uima.tools.jcasgen.Jg
if [ "$firstarg" = "" ]
then
  "$UIMA_JAVA_CALL" "$LOGGER" -cp "$UIMA_CLASSPATH" $MAIN
else
  if [ "$secondarg" = "" ]
  then
    "$UIMA_JAVA_CALL" "$LOGGER" -cp "$UIMA_CLASSPATH" $MAIN -jcasgeninput "$firstarg"
  else
    "$UIMA_JAVA_CALL" "$LOGGER" -cp "$UIMA_CLASSPATH" $MAIN -jcasgeninput "$firstarg" -jcasgenoutput "$secondarg"
  fi  
fi