@rem All this nonsense is necessary to remove quotes from the CLASSPATH and also handle the case where there is no CLASSPATH
@set _NOQUOTES=%CLASSPATH:"=%
@set _REALLYNOQUOTES=%_NOQUOTES:"=%
@if "%_REALLYNOQUOTES%"=="=" set _REALLYNOQUOTES=
set UIMA_CLASSPATH=%UIMA_CLASSPATH%;%UIMA_HOME%\docs\examples\resources;%UIMA_HOME%\lib\uima_core.jar;%UIMA_HOME%\lib\uima_cpe.jar;%UIMA_HOME%\lib\uima_jcas_builtin_types.jar;%UIMA_HOME%\lib\uima_tools.jar;%UIMA_HOME%\lib\uima_examples.jar;%UIMA_HOME%\lib\uima_adapter_messaging.jar;%UIMA_HOME%\lib\uima_adapter_soap.jar;%UIMA_HOME%\lib\uima_adapter_vinci.jar;%UIMA_HOME%\lib\uima_search.jar;%UIMA_HOME%\lib\juru.jar;%UIMA_HOME%\lib\siapi.jar;%CATALINA_HOME%\webapps\axis\WEB-INF\lib\activation.jar;%CATALINA_HOME%\webapps\axis\WEB-INF\lib\axis.jar;%CATALINA_HOME%\webapps\axis\WEB-INF\lib\commons-discovery.jar;%CATALINA_HOME%\webapps\axis\WEB-INF\lib\commons-discovery-0.2.jar;%CATALINA_HOME%\webapps\axis\WEB-INF\lib\commons-logging.jar;%CATALINA_HOME%\webapps\axis\WEB-INF\lib\commons-logging-1.0.4.jar;%CATALINA_HOME%\webapps\axis\WEB-INF\lib\jaxrpc.jar;%CATALINA_HOME%\webapps\axis\WEB-INF\lib\mail.jar;%CATALINA_HOME%\webapps\axis\WEB-INF\lib\saaj.jar;%UIMA_HOME%\lib\vinci\jVinci.jar;%UIMA_HOME%\lib\xml.jar;%UIMA_HOME%\lib\dltj50.jar;%UIMA_HOME%\lib\dltj50An.jar;%UIMA_HOME%\lib\icu4j.jar;%_REALLYNOQUOTES%;
@rem Also set VNS_HOST and VNS_PORT to default values if they are not specified
@if "%VNS_HOST%"=="" set VNS_HOST=localhost
@if "%VNS_PORT%"=="" set VNS_PORT=9000


