setlocal
call setUimaClassPath
if "%JAVA_HOME%"=="" set JAVA_HOME=%UIMA_HOME%\java\jre
"%JAVA_HOME%\bin\java" -cp "%UIMA_CLASSPATH%" "-Duima.datapath=%UIMA_DATAPATH%" -DVNS_HOST=%VNS_HOST% -DVNS_PORT=%VNS_PORT% "-Djava.util.logging.config.file=%UIMA_HOME%\Logger.properties" -Xms128M -Xmx256M org.apache.uima.examples.cpe.SimpleRunCPE %1 %2 %3 %4 %5 %6 %7 %8 %9


