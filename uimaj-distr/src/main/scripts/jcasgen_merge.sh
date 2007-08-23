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


#Modify next lines to set ECLIPSE_HOME to appropriate value in this exec if not available externally
if [ "$ECLIPSE_HOME" = "" ];
then
  ECLIPSE_HOME=
fi

#Modify next lines to set TEMP to appropriate value in this exec if not available externally
if [ "$TEMP" = "" ];
then
  TEMP=
fi

if [ "$ECLIPSE_HOME" = "" ];
then
  echo "ECLIPSE_HOME not set - please set it in this exec or externally"
else
  if [ "$TEMP" = "" ];
  then
    echo "TEMP not set - please set in this exec or externally"
  else
    ECLIPSE_TEMP_WORKSPACE=$TEMP/jcasgen_merge
    if [ $# -ge 1 ];
    then
      firstarg=$1
    fi
    if [ $# -ge 2 ];
    then
      secondarg=$2
    fi

    rm -rf $ECLIPSE_TEMP_WORKSPACE
    
    if [ "$JAVA_HOME" = "" ]
    then
      UIMA_JAVA_CALL=java
    else
      UIMA_JAVA_CALL="$JAVA_HOME/bin/java"
    fi    
    J="$UIMA_JAVA_CALL"
    ES="$ECLIPSE_HOME/startup.jar"
    MAIN=org.eclipse.core.launcher.Main
    LOGGER="-Djava.util.logging.config.file=$UIMA_HOME/config/FileConsoleLogger.properties"
    ARGS="-noupdate -nosplash -consolelog -application org.apache.uima.jcas.jcasgenp.JCasGen"
    if [ "$firstarg" = "" ] 
    then 
      "$J" "$LOGGER" -cp "$ES" $MAIN -data "$ECLIPSE_TEMP_WORKSPACE" $ARGS 
    else 
      if [ "$secondarg" = "" ]
      then
        "$J" "$LOGGER" -cp "$ES" $MAIN -data "$ECLIPSE_TEMP_WORKSPACE" $ARGS -jcasgeninput "$firstarg" 
      else
        "$J" "$LOGGER" -cp "$ES" $MAIN -data "$ECLIPSE_TEMP_WORKSPACE" $ARGS -jcasgeninput "$firstarg" -jcasgenoutput "$secondarg"
      fi
    fi   
  fi
fi
