#!/bin/sh
if [ "$JAVA_HOME" = "" ];
then
  JAVA_HOME=$UIMA_HOME/java/jre
fi
"$JAVA_HOME/bin/java" -cp "$UIMA_HOME/lib/uima_core.jar" org.apache.uima.internal.util.ReplaceStringInFiles "$UIMA_HOME/examples" .xml "C:/Program Files/apache/uima" "$UIMA_HOME" -ignorecase
"$JAVA_HOME/bin/java" -cp "$UIMA_HOME/lib/uima_core.jar" org.apache.uima.internal.util.ReplaceStringInFiles "$UIMA_HOME/examples" .classpath "C:/Program Files/apache/uima" "$UIMA_HOME" -ignorecase
"$JAVA_HOME/bin/java" -cp "$UIMA_HOME/lib/uima_core.jar" org.apache.uima.internal.util.ReplaceStringInFiles "$UIMA_HOME/examples" .launch "C:/Program Files/apache/uima" "$UIMA_HOME" -ignorecase
"$JAVA_HOME/bin/java" -cp "$UIMA_HOME/lib/uima_core.jar" org.apache.uima.internal.util.ReplaceStringInFiles "$UIMA_HOME/examples" .wsdd "C:/Program Files/apache/uima" "$UIMA_HOME" -ignorecase
"$JAVA_HOME/bin/java" -cp "$UIMA_HOME/lib/uima_core.jar" org.apache.uima.internal.util.ReplaceStringInFiles "$UIMA_HOME/examples" .xml "C:/Temp" temp -ignorecase
