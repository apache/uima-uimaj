@setlocal
@rem sample setting of ECLIPSE_HOME in next line
@rem @set ECLIPSE_HOME=C:\p\wswb\eclipse

@rem Remove quotes from ECLIPSE_HOME and also handle the case where there is no ECLIPSE_HOME
@set _NOQUOTES=%ECLIPSE_HOME:"=%
@set _REALLYNOQUOTES=%_NOQUOTES:"=%
@if "%_REALLYNOQUOTES%"=="=" set _REALLYNOQUOTES=
@set ECLIPSE_HOME=%_REALLYNOQUOTES%

@rem Modify next line to set ECLIPSE_HOME to appropriate value in this exec if not available externally
@if "%ECLIPSE_HOME%"=="" @set ECLIPSE_HOME=
@if "%ECLIPSE_HOME%"=="" @echo ECLIPSE_HOME not set - please set it in this exec or externally
@if "%ECLIPSE_HOME%"=="" goto exit1
@set ECLIPSE_TEMP_WORKSPACE=%TEMP%\jcasgen_merge

@if "%~1"=="" goto next1 
@set firstarg=-jcasgeninput "%~1"
@if "%~2"=="" goto next1
@set secondarg=-jcasgenoutput "%~2"
@:next1
@set ARGS=-data "%ECLIPSE_TEMP_WORKSPACE%" -noupdate -nosplash -consolelog -application org.apache.uima.jcas.jcasgenp.JCasGen %firstarg% %secondarg%
@set logger=-Djava.util.logging.config.file=%UIMA_HOME%/FileConsoleLogger.properties
@rmdir /S /Q "%ECLIPSE_TEMP_WORKSPACE%"
if "%JAVA_HOME%"=="" set JAVA_HOME=%UIMA_HOME%\java\jre
"%JAVA_HOME%\bin\java" "%logger%" -cp "%ECLIPSE_HOME%\startup.jar" org.eclipse.core.launcher.Main %ARGS%
@rem "%ECLIPSE_HOME%\eclipse.exe" -data %ECLIPSE_TEMP_WORKSPACE% -noupdate -nosplash -consolelog -application org.apache.uima.jcas.jcasgenp.JCasGen %firstarg% %secondarg%
@:exit1
