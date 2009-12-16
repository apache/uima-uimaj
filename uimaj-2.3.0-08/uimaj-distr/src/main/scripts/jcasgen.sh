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

if [ "$UIMA_HOME" = "" ]
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
echo "Running JCasGen with no Java CAS Model merging.  To run with merging, use jcasgen_merge (requires Eclipse, plus UIMA and EMF plugins)."
MAIN=org.apache.uima.tools.jcasgen.Jg
if [ "$firstarg" = "" ]
then
  "$UIMA_HOME/bin/runUimaClass.sh" $MAIN
else
  if [ "$secondarg" = "" ]
  then
    "$UIMA_HOME/bin/runUimaClass.sh" $MAIN -jcasgeninput "$firstarg"
  else
    "$UIMA_HOME/bin/runUimaClass.sh" $MAIN -jcasgeninput "$firstarg" -jcasgenoutput "$secondarg"
    fi  
fi