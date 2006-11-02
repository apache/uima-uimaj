#!/bin/sh
. "$UIMA_HOME/bin/setUimaClassPath.sh"
if [ "$JAVA_HOME" = "" ];
then
  JAVA_HOME=$UIMA_HOME/java/jre
fi
"$JAVA_HOME/bin/java" -cp "$UIMA_CLASSPATH:$CATALINA_HOME/webapps/axis/WEB-INF/classes" org.apache.axis.client.AdminClient -lhttp://localhost:8080/axis/services/AdminService $1
