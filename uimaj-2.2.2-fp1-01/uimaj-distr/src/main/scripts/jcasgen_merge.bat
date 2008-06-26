@echo off

REM   Licensed to the Apache Software Foundation (ASF) under one
REM   or more contributor license agreements.  See the NOTICE file
REM   distributed with this work for additional information
REM   regarding copyright ownership.  The ASF licenses this file
REM   to you under the Apache License, Version 2.0 (the
REM   "License"); you may not use this file except in compliance
REM   with the License.  You may obtain a copy of the License at
REM
REM    http://www.apache.org/licenses/LICENSE-2.0
REM
REM   Unless required by applicable law or agreed to in writing,
REM   software distributed under the License is distributed on an
REM   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
REM   KIND, either express or implied.  See the License for the
REM   specific language governing permissions and limitations
REM   under the License.

if not defined UIMA_HOME goto USAGE_UIMA
goto RUN

:USAGE_UIMA
echo UIMA_HOME environment variable is not set 
goto exit1

:RUN
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
@set logger=-Djava.util.logging.config.file=%UIMA_HOME%/config\FileConsoleLogger.properties
@rmdir /S /Q "%ECLIPSE_TEMP_WORKSPACE%"
if "%JAVA_HOME%"=="" (set UIMA_JAVA_CALL=java) else (set UIMA_JAVA_CALL=%JAVA_HOME%\bin\java)
"%UIMA_JAVA_CALL%" "%logger%" -cp "%ECLIPSE_HOME%\startup.jar" org.eclipse.core.launcher.Main %ARGS%
@rem "%ECLIPSE_HOME%\eclipse.exe" -data %ECLIPSE_TEMP_WORKSPACE% -noupdate -nosplash -consolelog -application org.apache.uima.jcas.jcasgenp.JCasGen %firstarg% %secondarg%
@:exit1
