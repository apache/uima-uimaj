#!/bin/sh
if [ "$JAVA_HOME" = "" ];
then
  JAVA_HOME=$UIMA_HOME/java/jre
fi
"$JAVA_HOME/bin/java" -cp "$UIMA_HOME/lib/uima_core.jar" org.apache.uima.util.ReplaceStringInFiles "$UIMA_HOME/docs/examples" .xml "C:/Program Files/IBM/uima" "$UIMA_HOME" -ignorecase
"$JAVA_HOME/bin/java" -cp "$UIMA_HOME/lib/uima_core.jar" org.apache.uima.util.ReplaceStringInFiles "$UIMA_HOME/docs/examples" .classpath "C:/Program Files/IBM/uima" "$UIMA_HOME" -ignorecase
"$JAVA_HOME/bin/java" -cp "$UIMA_HOME/lib/uima_core.jar" org.apache.uima.util.ReplaceStringInFiles "$UIMA_HOME/docs/examples" .launch "C:/Program Files/IBM/uima" "$UIMA_HOME" -ignorecase
"$JAVA_HOME/bin/java" -cp "$UIMA_HOME/lib/uima_core.jar" org.apache.uima.util.ReplaceStringInFiles "$UIMA_HOME/docs/examples" .wsdd "C:/Program Files/IBM/uima" "$UIMA_HOME" -ignorecase
"$JAVA_HOME/bin/java" -cp "$UIMA_HOME/lib/uima_core.jar" org.apache.uima.util.ReplaceStringInFiles "$UIMA_HOME/docs/examples" .xml "C:/Temp" temp -ignorecase
