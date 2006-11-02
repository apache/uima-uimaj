setlocal
call "%UIMA_HOME%\bin\setUimaClassPath"
if "%JAVA_HOME%"=="" set JAVA_HOME=%UIMA_HOME%\java\jre
"%JAVA_HOME%\bin\java" -cp "%UIMA_CLASSPATH%;%CATALINA_HOME%\webapps\axis\WEB-INF\classes" org.apache.axis.client.AdminClient -lhttp://localhost:8080/axis/services/AdminService %1


