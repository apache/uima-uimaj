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

UIMA_CLASSPATH=$UIMA_CLASSPATH:$UIMA_HOME/docs/examples/resources:$UIMA_HOME/lib/uima-core.jar:$UIMA_HOME/lib/uima-document-annotation.jar:$UIMA_HOME/lib/uima-cpe.jar:$UIMA_HOME/lib/uima-tools.jar:$UIMA_HOME/lib/uima-examples.jar:$UIMA_HOME/lib/uima-adapter-soap.jar:$UIMA_HOME/lib/uima-adapter-vinci.jar:$CATALINA_HOME/webapps/axis/WEB-INF/lib/axis.jar:$CATALINA_HOME/webapps/axis/WEB-INF/lib/commons-discovery.jar:$CATALINA_HOME/webapps/axis/WEB-INF/lib/commons-discovery-0.2.jar:$CATALINA_HOME/webapps/axis/WEB-INF/lib/commons-logging.jar:$CATALINA_HOME/webapps/axis/WEB-INF/lib/commons-logging-1.0.4.jar:$CATALINA_HOME/webapps/axis/WEB-INF/lib/jaxrpc.jar:$CATALINA_HOME/webapps/axis/WEB-INF/lib/saaj.jar:$CATALINA_HOME/webapps/axis/WEB-INF/lib/activation.jar:$UIMA_HOME/lib/jVinci.jar:$CLASSPATH
#also set default values for VNS_HOST and VNS_PORT
if [ "$VNS_HOST" = "" ];
then
  VNS_HOST=localhost
fi
if [ "$VNS_PORT" = "" ];
then
  VNS_PORT=9000
fi