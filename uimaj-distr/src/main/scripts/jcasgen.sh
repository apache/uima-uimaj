#!/bin/sh -vx
if [ $# -ge 1 ]
then
  firstarg=$1
fi
if [ $# -ge 2 ]
then
  secondarg=$2
fi
. "$UIMA_HOME/bin/setUimaClassPath.sh"
echo "Running JCasGen with no Java CAS Model merging.  To run with merging, use jcasgen_merge (requires Eclipse, plus UIMA and EMF plugins)."
if [ "$JAVA_HOME" = "" ]
then
  JAVA_HOME=$UIMA_HOME/java/jre
fi
LOGGER="-Djava.util.logging.config.file=$UIMA_HOME/FileConsoleLogger.properties"
MAIN=org.apache.uima.jcas.jcasgen_gen.Jg
if [ "$firstarg" = "" ]
then
  "$JAVA_HOME/bin/java" "$LOGGER" -cp "$UIMA_CLASSPATH" $MAIN
else
  if [ "$secondarg" = "" ]
  then
    "$JAVA_HOME/bin/java" "$LOGGER" -cp "$UIMA_CLASSPATH" $MAIN -jcasgeninput "$firstarg"
  else
    "$JAVA_HOME/bin/java" "$LOGGER" -cp "$UIMA_CLASSPATH" $MAIN -jcasgeninput "$firstarg" -jcasgenoutput "$secondarg"
  fi  
fi