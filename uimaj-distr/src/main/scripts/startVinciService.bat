@setlocal
@if "%~1"=="" goto usage
@call "%UIMA_HOME%\bin\setUimaClassPath"
@if "%JAVA_HOME%"=="" set JAVA_HOME=%UIMA_HOME%\java\jre

@set SERVICE=%~1
@set INSTANCEID=0

@if NOT "%~3"=="" goto execute2

@if "%~2"=="" goto execute
@set VNS_HOST=%~2
@:execute
"%JAVA_HOME%\bin\java" -cp "%UIMA_CLASSPATH%" "-Duima.datapath=%UIMA_DATAPATH%" -DVNS_HOST=%VNS_HOST% -DVNS_PORT=%VNS_PORT% "-Djava.util.logging.config.file=%UIMA_HOME%\Logger.properties" org.apache.uima.adapter.vinci.VinciCasObjectProcessorService_impl %SERVICE%
@goto end

@:execute2
@set VNS_HOST=%~2
@set INSTANCEID=%~3
"%JAVA_HOME%\bin\java" -cp "%UIMA_CLASSPATH%" "-Duima.datapath=%UIMA_DATAPATH%" -DVNS_HOST=%VNS_HOST% -DVNS_PORT=%VNS_PORT% "-Djava.util.logging.config.file=%UIMA_HOME%\Logger.properties" org.apache.uima.adapter.vinci.VinciCasObjectProcessorService_impl %SERVICE% %INSTANCEID%
@goto end

@:usage
@  echo Usage: startVinciService.sh svcdescriptor [vns_host]
@:end

