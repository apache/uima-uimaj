@echo off
if not exist %UIMA_HOME% goto USAGE_UIMA
goto RUN

:USAGE_UIMA
echo UIMA_HOME environment variable is not set 
goto EXIT

:RUN
setlocal
@echo on
call "%UIMA_HOME%\bin\setUimaClassPath"
if "%JAVA_HOME%"=="" set JAVA_HOME=%UIMA_HOME%\java\jre
"%JAVA_HOME%\bin\java" -cp "%UIMA_CLASSPATH%" -Duima.home="%UIMA_HOME%" org.apache.uima.tools.pear.install.InstallPear
:EXIT