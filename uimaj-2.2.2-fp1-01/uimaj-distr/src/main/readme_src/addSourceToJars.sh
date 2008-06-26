#!/bin/sh

#   Licensed to the Apache Software Foundation (ASF) under one
#   or more contributor license agreements.  See the NOTICE file
#   distributed with this work for additional information
#   regarding copyright ownership.  The ASF licenses this file
#   to you under the Apache License, Version 2.0 (the
#   "License"); you may not use this file except in compliance
#   with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing,
#   software distributed under the License is distributed on an
#   #  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#   KIND, either express or implied.  See the License for the
#   specific language governing permissions and limitations
#   under the License.

cd jVinci/src/main/java
jar -uvf $UIMA_HOME/lib/jVinci.jar *

cd ../../../../uimaj-adapter-soap/src/main/java
jar -uvf $UIMA_HOME/lib/uima-adapter-soap.jar *

cd ../../../../uimaj-adapter-vinci/src/main/java
jar -uvf $UIMA_HOME/lib/uima-adapter-vinci.jar *

cd ../../../../uimaj-core/src/main/java
jar -uvf $UIMA_HOME/lib/uima-core.jar *

cd ../../../../uimaj-cpe/src/main/java
jar -uvf $UIMA_HOME/lib/uima-cpe.jar *

cd ../../../../uimaj-document-annotation/src/main/java
jar -uvf $UIMA_HOME/lib/uima-document-annotation.jar *

cd ../../../../uimaj-examples/src/main/java
jar -uvf $UIMA_HOME/lib/uima-examples.jar *

cd ../../../../uimaj-tools/src/main/java
jar -uvf $UIMA_HOME/lib/uima-tools.jar *

cd ../../../..
