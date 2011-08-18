@echo off
@REM   Licensed to the Apache Software Foundation (ASF) under one
@REM   or more contributor license agreements.  See the NOTICE file
@REM   distributed with this work for additional information
@REM   regarding copyright ownership.  The ASF licenses this file
@REM   to you under the Apache License, Version 2.0 (the
@REM   "License"); you may not use this file except in compliance
@REM   with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM   Unless required by applicable law or agreed to in writing,
@REM   software distributed under the License is distributed on an
@REM   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM   KIND, either express or implied.  See the License for the
@REM   specific language governing permissions and limitations
@REM   under the License.

@if not defined UIMA_HOME (
 @echo UIMA_HOME environment variable is not set 
 @exit /B 1
)

@setlocal

@rem Set ActiveMQ home
@if "%ACTIVEMQ_HOME%"=="" (set "ACTIVEMQ_HOME=%UIMA_HOME%\apache-activemq-5.4.1")

@if "%JAVA_HOME%"=="" (set UIMA_JAVA_CALL=java) else (set "UIMA_JAVA_CALL=%JAVA_HOME%\bin\java")

@rem Set jar search order of: UIMA_CLASSPATH uima activemq(optional) tomcat(optional) CLASSPATH
@set UIMA_CLASSPATH=%UIMA_CLASSPATH%;%UIMA_HOME%\examples\resources
@set UIMA_CLASSPATH=%UIMA_CLASSPATH%;%UIMA_HOME%\lib
@if EXIST "%ACTIVEMQ_HOME%" (set "UIMA_CLASSPATH=%UIMA_CLASSPATH%;%ACTIVEMQ_HOME%;%ACTIVEMQ_HOME%\lib;%ACTIVEMQ_HOME%\lib\optional")
@if NOT "%CATALINA_HOME%"=="" (set "UIMA_CLASSPATH=%UIMA_CLASSPATH%;%CATALINA_HOME%\webapps\axis\WEB-INF\lib;%CATALINA_HOME%\webapps\axis\WEB-INF\classes")
@set UIMA_CLASSPATH=%UIMA_CLASSPATH%;%CLASSPATH%

@rem Set path to support running C++ annotators
@if "%UIMACPP_HOME%"=="" set UIMACPP_HOME=%UIMA_HOME%\uimacpp
@set PATH=%UIMACPP_HOME%\bin;%UIMACPP_HOME%\examples\tutorial\src;%PATH%

@rem Set VNS_HOST and VNS_PORT to default values if they are not specified
@if "%VNS_HOST%"=="" set VNS_HOST=localhost
@if "%VNS_PORT%"=="" set VNS_PORT=9000

@rem Set default for logger properties
@if "%UIMA_LOGGER_CONFIG_FILE%"=="" set UIMA_LOGGER_CONFIG_FILE=%UIMA_HOME%\config\Logger.properties

@rem Set default JVM opts
@if "%UIMA_JVM_OPTS%"=="" set UIMA_JVM_OPTS=-Xms128M -Xmx800M

@rem Check if Uima AS is installed, and if so set the default log4j configuration file
@if EXIST "%UIMA_HOME%"\as_config (set "LOG4J_CONFIG_FILE=-Dlog4j.configuration=file:%UIMA_HOME%\as_config\uimaAsLog4j.properties") else (set LOG4J_CONFIG_FILE=-DNoOp)
   
@if "%UIMA_CVDMAN%"=="" set UIMA_CVDMAN=-Duima.tools.cvd.manpath.notset

@rem Finally load the jars and run the class
@"%UIMA_JAVA_CALL%" -DVNS_HOST=%VNS_HOST% -DVNS_PORT=%VNS_PORT% "-Duima.home=%UIMA_HOME%" "-Duima.datapath=%UIMA_DATAPATH%" "-Djava.util.logging.config.file=%UIMA_LOGGER_CONFIG_FILE%" "%UIMA_CVDMAN%" %UIMA_JVM_OPTS% "%LOG4J_CONFIG_FILE%" -DUimaBootstrapSuppressClassPathDisplay -Dorg.apache.uima.jarpath="%UIMA_CLASSPATH%" -jar "%UIMA_HOME%\lib\uimaj-bootstrap.jar" %*
