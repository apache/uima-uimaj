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

# set default ActiveMQ home 
if [ "$ACTIVEMQ_HOME" = "" ]
then
  ACTIVEMQ_HOME=$UIMA_HOME/apache-activemq-5.4.1
fi

if [ "$JAVA_HOME" = "" ]
then
  UIMA_JAVA_CALL=java
else
  UIMA_JAVA_CALL="$JAVA_HOME/bin/java"
fi

#Set jar search order of: UIMA_CLASSPATH uima activemq(optional) tomcat(optional) CLASSPATH
UIMA_CLASSPATH=$UIMA_CLASSPATH:$UIMA_HOME/examples/resources
UIMA_CLASSPATH=$UIMA_CLASSPATH:$UIMA_HOME/lib
if [ -e "$ACTIVEMQ_HOME" ] 
then
	UIMA_CLASSPATH=$UIMA_CLASSPATH:$ACTIVEMQ_HOME:$ACTIVEMQ_HOME/lib:$ACTIVEMQ_HOME/lib/optional
fi

# -n: true if string has non-zero length
if [ -n "$CATALINA_HOME" ]
then
	UIMA_CLASSPATH=$UIMA_CLASSPATH:$CATALINA_HOME/webapps/axis/WEB-INF/lib:$CATALINA_HOME/webapps/axis/WEB-INF/classes
fi

UIMA_CLASSPATH=$UIMA_CLASSPATH:$CLASSPATH

#set LD_LIBRARY_PATH to support running C++ annotators
if [ "$UIMACPP_HOME" = "" ]
then
  UIMACPP_HOME=$UIMA_HOME/uimacpp
fi

# The exports here are done in 2 lines because on some shells (dash)
# (used by Ubuntu) blanks in the substitutions (e.g., a dir whose name
# includes a blank) causes a failure
PATH=$UIMACPP_HOME/bin:$UIMACPP_HOME/examples/tutorial/src:$PATH
export PATH

LD_LIBRARY_PATH=$UIMACPP_HOME/lib:$UIMACPP_HOME/examples/tutorial/src:$LD_LIBRARY_PATH
export LD_LIBRARY_PATH

#also set DYLD_LIBRARY_PATH, used by Mac OSX
DYLD_LIBRARY_PATH=$UIMACPP_HOME/lib:$UIMACPP_HOME/examples/tutorial/src:$DYLD_LIBRARY_PATH
export DYLD_LIBRARY_PATH

#also set default values for VNS_HOST and VNS_PORT
if [ "$VNS_HOST" = "" ];
then
  VNS_HOST=localhost
fi
if [ "$VNS_PORT" = "" ];
then
  VNS_PORT=9000
fi
#also set default vlaue for UIMA_LOGGER_CONFIG_FILE
if [ "$UIMA_LOGGER_CONFIG_FILE" = "" ]
then
  UIMA_LOGGER_CONFIG_FILE=$UIMA_HOME/config/Logger.properties
fi
#set default JVM opts
if [ "$UIMA_JVM_OPTS" = "" ]
then
  UIMA_JVM_OPTS="-Xms128M -Xmx800M"
fi

# Check if Uima AS is installed, and if so set the default log4j configuration file
if [ -e "$UIMA_HOME"/as_config ] 
then
	LOG4J_CONFIG_FILE=-Dlog4j.configuration=file:$UIMA_HOME/as_config/uimaAsLog4j.properties
else
#   Define a variable with an arbitrary no op value 
	LOG4J_CONFIG_FILE=-DNoOp
fi

if [ "$UIMA_CVDMAN" = "" ]
then 
  UIMA_CVDMAN=-Duima.tools.cvd.manpath.notset
fi

# Finally load the jars and run the class
"$UIMA_JAVA_CALL" -DVNS_HOST=$VNS_HOST -DVNS_PORT=$VNS_PORT "-Duima.home=$UIMA_HOME" "-Duima.datapath=$UIMA_DATAPATH" "-Djava.util.logging.config.file=$UIMA_LOGGER_CONFIG_FILE" "$UIMA_CVDMAN" $UIMA_JVM_OPTS "$LOG4J_CONFIG_FILE" -DUimaBootstrapSuppressClassPathDisplay -Dorg.apache.uima.jarpath="$UIMA_CLASSPATH" -jar "$UIMA_HOME/lib/uimaj-bootstrap.jar" $@

