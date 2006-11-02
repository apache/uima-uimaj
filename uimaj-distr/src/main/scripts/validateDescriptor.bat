setlocal
call "%UIMA_HOME%\bin\setUimaClassPath"
if "%JAVA_HOME%"=="" set JAVA_HOME=%UIMA_HOME%\java\jre
"%JAVA_HOME%\bin\java" -cp "%UIMA_CLASSPATH%" "-Duima.datapath=%UIMA_DATAPATH%" -Xms128M -Xmx256M org.apache.uima.tools.ValidateDescriptor %1 %2 %3 %4 %5 %6 %7 %8 %9


