@setlocal
@if "%~1"=="" goto next1 
@set firstarg=-jcasgeninput "%~1"
@if "%~2"=="" goto next1
@set secondarg=-jcasgenoutput "%~2"
@:next1
@call "%UIMA_HOME%\bin\setUimaClasspath"
@set LOGGER=-Djava.util.logging.config.file=%UIMA_HOME%\FileConsoleLogger.properties
@echo Running JCasGen with no Java CAS Model merging.  To run with merging, use jcasgen_merge (requires Eclipse, plus UIMA and EMF plugins).
@if "%JAVA_HOME%"=="" set JAVA_HOME=%UIMA_HOME%\java\jre
"%JAVA_HOME%\bin\java" "%LOGGER%" -cp "%UIMA_CLASSPATH%" org.apache.uima.jcas.jcasgen_gen.Jg %firstarg% %secondarg%
