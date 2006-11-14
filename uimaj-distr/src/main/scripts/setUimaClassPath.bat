@rem All this nonsense is necessary to remove quotes from the CLASSPATH and also handle the case where there is no CLASSPATH
@set _NOQUOTES=%CLASSPATH:"=%
@set _REALLYNOQUOTES=%_NOQUOTES:"=%
@if "%_REALLYNOQUOTES%"=="=" set _REALLYNOQUOTES=
set UIMA_CLASSPATH=%UIMA_CLASSPATH%;%UIMA_HOME%\docs\examples\resources;%UIMA_HOME%\lib\uima-core.jar;%UIMA_HOME%\lib\uima-cpe.jar;%UIMA_HOME%\lib\uima-tools.jar;%UIMA_HOME%\lib\uima-examples.jar;%UIMA_HOME%\lib\uima-adapter-soap.jar;%UIMA_HOME%\lib\uima-adapter-vinci.jar;%CATALINA_HOME%\webapps\axis\WEB-INF\lib\activation.jar;%CATALINA_HOME%\webapps\axis\WEB-INF\lib\axis.jar;%CATALINA_HOME%\webapps\axis\WEB-INF\lib\commons-discovery.jar;%CATALINA_HOME%\webapps\axis\WEB-INF\lib\commons-discovery-0.2.jar;%CATALINA_HOME%\webapps\axis\WEB-INF\lib\commons-logging.jar;%CATALINA_HOME%\webapps\axis\WEB-INF\lib\commons-logging-1.0.4.jar;%CATALINA_HOME%\webapps\axis\WEB-INF\lib\jaxrpc.jar;%CATALINA_HOME%\webapps\axis\WEB-INF\lib\mail.jar;%CATALINA_HOME%\webapps\axis\WEB-INF\lib\saaj.jar;%UIMA_HOME%\lib\jVinci.jar;%_REALLYNOQUOTES%;
@rem Also set VNS_HOST and VNS_PORT to default values if they are not specified
@if "%VNS_HOST%"=="" set VNS_HOST=localhost
@if "%VNS_PORT%"=="" set VNS_PORT=9000


