#/bin/sh
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# Formatter settings
JDT_CORE_PREFS="src/main/eclipse/org.eclipse.jdt.core.prefs"

# Save actions
JDT_UI_PREFS="src/main/eclipse/org.eclipse.jdt.ui.prefs"

function installPrefs {
  mkdir -p $1/.settings/
  cp -v $JDT_CORE_PREFS $1/.settings/
  cp -v $JDT_UI_PREFS $1/.settings/
}

installPrefs .
installPrefs aggregate-uimaj-docbooks
installPrefs uimaj-ep-debug
installPrefs jVinci
installPrefs uima-docbook-tutorials-and-users-guides
installPrefs PearPackagingMavenPlugin
installPrefs uimaj-test-util
installPrefs uimaj-adapter-soap
installPrefs uimaj-eclipse-feature-runtime
installPrefs uima-docbook-references
installPrefs uimaj-ep-cas-editor
installPrefs uimaj-tools
installPrefs uimaj-ep-jcasgen
installPrefs uimaj-adapter-vinci
installPrefs uimaj-ep-cas-editor-ide
installPrefs uimaj-json
installPrefs uimaj-eclipse-update-site
installPrefs uimaj-ep-configurator
installPrefs uimaj-component-test-util
installPrefs uimaj-document-annotation
installPrefs uimaj-ep-runtime
installPrefs jcasgen-maven-plugin
installPrefs jcasgen-maven-plugin/src/test/resources/classpath
installPrefs jcasgen-maven-plugin/src/test/resources/exclude
installPrefs jcasgen-maven-plugin/src/test/resources/simple
installPrefs jcasgen-maven-plugin/src/test/resources/invalidFeature
installPrefs jcasgen-maven-plugin/src/test/resources/wildcard
installPrefs aggregate-uimaj-eclipse-plugins
installPrefs uimaj-v3migration-jcas
installPrefs uimaj-examples
installPrefs uimaj-ep-launcher
installPrefs uimaj-bootstrap
installPrefs uimaj-core
installPrefs uimaj-parent
installPrefs uimaj-ep-pear-packager
installPrefs aggregate-uimaj
installPrefs uima-docbook-overview-and-setup
installPrefs uima-doc-v3-users-guide
installPrefs uima-docbook-tools
installPrefs uimaj-eclipse-feature-tools
installPrefs uimaj-cpe
