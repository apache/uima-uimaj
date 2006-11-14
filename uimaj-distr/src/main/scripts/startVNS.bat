setlocal
if "%JAVA_HOME%"=="" set JAVA_HOME=%UIMA_HOME%\java\jre
"%JAVA_HOME%\bin\java" -cp "%UIMA_HOME%/lib/jVinci.jar" org.apache.vinci.transport.vns.service.VNS %1 %2 %3 %4 %5 %6 %7 %8 %9

