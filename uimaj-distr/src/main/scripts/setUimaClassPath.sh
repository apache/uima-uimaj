#!/bin/sh
UIMA_CLASSPATH=$UIMA_CLASSPATH:$UIMA_HOME/docs/examples/resources:$UIMA_HOME/lib/uima-core.jar:$UIMA_HOME/lib/uima-cpe.jar:$UIMA_HOME/lib/uima-tools.jar:$UIMA_HOME/lib/uima-examples.jar:$UIMA_HOME/lib/uima-adapter-soap.jar:$UIMA_HOME/lib/uima-adapter-vinci.jar:$CATALINA_HOME/webapps/axis/WEB-INF/lib/axis.jar:$CATALINA_HOME/webapps/axis/WEB-INF/lib/commons-discovery.jar:$CATALINA_HOME/webapps/axis/WEB-INF/lib/commons-discovery-0.2.jar:$CATALINA_HOME/webapps/axis/WEB-INF/lib/commons-logging.jar:$CATALINA_HOME/webapps/axis/WEB-INF/lib/commons-logging-1.0.4.jar:$CATALINA_HOME/webapps/axis/WEB-INF/lib/jaxrpc.jar:$CATALINA_HOME/webapps/axis/WEB-INF/lib/saaj.jar:$CATALINA_HOME/webapps/axis/WEB-INF/lib/activation.jar:$UIMA_HOME/lib/jVinci.jar:$CLASSPATH
#also set default values for VNS_HOST and VNS_PORT
if [ "$VNS_HOST" = "" ];
then
  VNS_HOST=localhost
fi
if [ "$VNS_PORT" = "" ];
then
  VNS_PORT=9000
fi