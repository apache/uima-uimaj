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

REM Run with -notest to skip the unit tests

@echo on

@if "%~1"=="" goto usage
@if "%~1"=="trunk" goto trunk
@set level=tags/%~1
@set leveldir=%~1
@goto checkargs

@:trunk
@set level=trunk
@set leveldir=trunk
@goto checkargs

@:checkargs
@set jvmarg=
@set mvnCommand=clean install
@if not "%3"=="" goto usage
@if "%~2"=="" goto execute
@if "%~2"=="-notest" goto notest
@if "%~2"=="-deploy" goto deploy
@goto usage

@:usage
@echo off
echo Usage: extractAndBuild.bat level [-notest] [-deploy]
echo            (-notest and -deploy cannot be used together)
@echo on
@goto exit

@:notest
@set jvmarg="-Dmaven.test.skip=true"
@goto execute

@:deploy
@set jvmarg="-DsignArtifacts=true"
@set mvnCommand=source:jar deploy
@goto execute

@:execute
svn checkout -r HEAD http://svn.apache.org/repos/asf/incubator/uima/uimaj/%level%
cd %leveldir%
cd uimaj
call mvn %jvmarg%  -Duima.build.date="%date% %time%" %mvnCommand%
cd ..
cd uimaj-distr
call mvn clean assembly:assembly

@:exit
