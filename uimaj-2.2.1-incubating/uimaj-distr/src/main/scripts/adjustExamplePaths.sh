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

if [ "$JAVA_HOME" = "" ]
then
  UIMA_JAVA_CALL=java
else
  UIMA_JAVA_CALL="$JAVA_HOME/bin/java"
fi
"$UIMA_JAVA_CALL" -cp "$UIMA_HOME/lib/uima-core.jar" org.apache.uima.internal.util.ReplaceStringInFiles "$UIMA_HOME/examples" .xml "C:/Program Files/apache-uima" "$UIMA_HOME" -ignorecase
"$UIMA_JAVA_CALL" -cp "$UIMA_HOME/lib/uima-core.jar" org.apache.uima.internal.util.ReplaceStringInFiles "$UIMA_HOME/examples" .classpath "C:/Program Files/apache-uima" "$UIMA_HOME" -ignorecase
"$UIMA_JAVA_CALL" -cp "$UIMA_HOME/lib/uima-core.jar" org.apache.uima.internal.util.ReplaceStringInFiles "$UIMA_HOME/examples" .launch "C:/Program Files/apache-uima" "$UIMA_HOME" -ignorecase
"$UIMA_JAVA_CALL" -cp "$UIMA_HOME/lib/uima-core.jar" org.apache.uima.internal.util.ReplaceStringInFiles "$UIMA_HOME/examples" .wsdd "C:/Program Files/apache-uima" "$UIMA_HOME" -ignorecase
