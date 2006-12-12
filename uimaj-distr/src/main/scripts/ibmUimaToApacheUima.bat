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

@echo on

if "%JAVA_HOME%"=="" set JAVA_HOME=%UIMA_HOME%\java\jre
REM   update package names in import statements
"%JAVA_HOME%\bin\java" -cp "%UIMA_HOME%/lib/uima-core.jar;%UIMA_HOME%/lib/uima-tools.jar" org.apache.uima.tools.migration.IbmToApachePackageNames %1

REM   update frameworkImplementation elements in XML descriptors
"%JAVA_HOME%\bin\java" -cp "%UIMA_HOME%/lib/uima-core.jar" org.apache.uima.internal.util.ReplaceStringInFiles %1 .xml com.ibm.uima.java org.apache.uima.java
"%JAVA_HOME%\bin\java" -cp "%UIMA_HOME%/lib/uima-core.jar" org.apache.uima.internal.util.ReplaceStringInFiles %1 .xml JEDII org.apache.uima.java

REM miscellaneous other replacements
"%JAVA_HOME%\bin\java" -cp "%UIMA_HOME%/lib/uima-core.jar" org.apache.uima.internal.util.ReplaceStringInFiles %1 "" VinciCasObjectProcessorService_impl VinciAnalysisEngineService_impl
"%JAVA_HOME%\bin\java" -cp "%UIMA_HOME%/lib/uima-core.jar" org.apache.uima.internal.util.ReplaceStringInFiles %1 "" org.apache.uima.tools.DocumentAnalyzer org.apache.uima.tools.docanalyzer.DocumentAnalyzer
