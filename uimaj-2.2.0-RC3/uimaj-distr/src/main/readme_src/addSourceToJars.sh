#!/bin/sh

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
