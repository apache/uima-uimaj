#!/bin/sh -vx
#Modify next lines to set ECLIPSE_HOME to appropriate value in this exec if not available externally
if [ "$ECLIPSE_HOME" = "" ];
then
  ECLIPSE_HOME=
fi

#Modify next lines to set TEMP to appropriate value in this exec if not available externally
if [ "$TEMP" = "" ];
then
  TEMP=
fi

if [ "$ECLIPSE_HOME" = "" ];
then
  echo "ECLIPSE_HOME not set - please set it in this exec or externally"
else
  if [ "$TEMP" = "" ];
  then
    echo "TEMP not set - please set in this exec or externally"
  else
    ECLIPSE_TEMP_WORKSPACE=$TEMP/jcasgen_merge
    if [ $# -ge 1 ];
    then
      firstarg=$1
    fi
    if [ $# -ge 2 ];
    then
      secondarg=$2
    fi

    rm -rf $ECLIPSE_TEMP_WORKSPACE
    
    if [ "$JAVA_HOME" = "" ];
    then
      JAVA_HOME=$UIMA_HOME/java/jre
    fi
    J="$JAVA_HOME/bin/java"
    ES="$ECLIPSE_HOME/startup.jar"
    MAIN=org.eclipse.core.launcher.Main
    LOGGER="-Djava.util.logging.config.file=$UIMA_HOME/FileConsoleLogger.properties"
    ARGS="-noupdate -nosplash -consolelog -application org.apache.uima.jcas.jcasgenp.JCasGen"
    if [ "$firstarg" = "" ] 
    then 
      "$J" "$LOGGER" -cp "$ES" $MAIN -data "$ECLIPSE_TEMP_WORKSPACE" $ARGS 
    else 
      if [ "$secondarg" = "" ]
      then
        "$J" "$LOGGER" -cp "$ES" $MAIN -data "$ECLIPSE_TEMP_WORKSPACE" $ARGS -jcasgeninput "$firstarg" 
      else
        "$J" "$LOGGER" -cp "$ES" $MAIN -data "$ECLIPSE_TEMP_WORKSPACE" $ARGS -jcasgeninput "$firstarg" -jcasgenoutput "$secondarg"
      fi
    fi   
  fi
fi
