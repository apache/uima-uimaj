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

cd jVinci\src\main\java
jar -uvf %UIMA_HOME%\lib\jVinci.jar *

cd ..\..\..\..\uimaj-adapter-soap\src\main\java
jar -uvf %UIMA_HOME%\lib\uima-adapter-soap.jar *

cd ..\..\..\..\uimaj-adapter-vinci\src\main\java
jar -uvf %UIMA_HOME%\lib\uima-adapter-vinci.jar *

cd ..\..\..\..\uimaj-core\src\main\java
jar -uvf %UIMA_HOME%\lib\uima-core.jar *

cd ..\..\..\..\uimaj-cpe\src\main\java
jar -uvf %UIMA_HOME%\lib\uima-cpe.jar *

cd ..\..\..\..\uimaj-document-annotation\src\main\java
jar -uvf %UIMA_HOME%\lib\uima-document-annotation.jar *

cd ..\..\..\..\uimaj-examples\src\main\java
jar -uvf %UIMA_HOME%\lib\uima-examples.jar *

cd ..\..\..\..\uimaj-tools\src\main\java
jar -uvf %UIMA_HOME%\lib\uima-tools.jar *

cd ..\..\..\..