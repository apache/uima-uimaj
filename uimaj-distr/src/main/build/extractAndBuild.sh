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
#   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#   KIND, either express or implied.  See the License for the
#   specific language governing permissions and limitations
#   under the License.

# Bourne shell syntax, this should hopefully run on pretty much anything.

usage() {
  echo "Usage: extractAndBuild.sh [-notest] [-deploy]"
  echo "           (-notest and -deploy cannot be used together)"
}

vmargs=""
mvnCommand=install
# Check for -notest switch.  If present, add the no-test define to the mvn command line.
if [ -n $1 ]
  then
  if [ $1 = "-notest" ]
  then
    vmargs="-Dmaven.test.skip=true"
  elif [ $1 = "-deploy" ]
  then
    mvnCommand="source:jar deploy"
  else
    usage
    exit 1
  fi
fi

svn checkout http://svn.apache.org/repos/asf/incubator/uima/uimaj/trunk
cd trunk/uimaj
mvn ${vmargs} -Duima.build.date="`date`" $mvnCommand
cd ..
cd uimaj-distr
mvn assembly:assembly

