setlocal
call "%UIMA_HOME%\bin\setUimaClassPath"
if "%JAVA_HOME%"=="" set JAVA_HOME=%UIMA_HOME%\java\jre
"%JAVA_HOME%\bin\java" -cp "%UIMA_CLASSPATH%" -Xms128M -Xmx256M "-Duima.home=%UIMA_HOME%" "-Duima.datapath=%UIMA_DATAPATH%" "-Djava.util.logging.config.file=%UIMA_HOME%\Logger.properties" org.apache.uima.tools.XCasAnnotationViewerMain


