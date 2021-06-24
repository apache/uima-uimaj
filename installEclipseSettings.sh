#/bin/sh

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
