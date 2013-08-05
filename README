
      Apache UIMA (Unstructured Information Management Architecture) v2.4.2 SDK
      -------------------------------------------------------------------------

Building from the Source Distribution
-------------------------------------

We use Maven 3.0 or later for building; download this if needed, 
and set the environment variable MAVEN_OPTS to -Xmx800m -XX:MaxPerSize=256m.

Then do the build by going into the .../uimaj directory, and issuing the command
   mvn clean install
   
This builds everything except the ...source-release.zip file. If you want that,
change the command to 

   mvn clean install -Papache-release
   
Look for the result here: 
   target/uimaj-[version]-source-release.zip (if run with -Papache-release)

For more details, please see http://uima.apache.org/building-uima.html   

What's New in 2.4.2
-------------------

  This is a bug fix release, only.    
    
Supported Platforms
--------------------

Apache UIMA requires Java level 1.5; it has been tested with Sun/Oracle Java SDK v5 and v6 amd v7, and IBM Java 6 and 7.
Running the Eclipse plugin tooling for UIMA requires you start Eclipse using a Java 5 or later, as well.
The supported platforms are: Windows, Linux, Solaris, AIX and Mac OS X.  
Other platforms and Java (5+) implementations should work, but have not been significantly tested.

Many of the scripts in the /bin directory invoke Java. They use the value of the environment variable, JAVA_HOME, 
to locate the Java to use; if it is not set, they invoke "java" expecting to find an appropriate Java in your PATH. 


Environment Variables
----------------------

After you have unpacked the Apache UIMA distribution from the package of your choice (e.g. .zip or .gz), 
perform the steps below to set up UIMA so that it will function properly.

    * Set JAVA_HOME to the directory of your JRE installation you would like to use for UIMA.  
    * Set UIMA_HOME to the apache-uima directory of your unpacked Apache UIMA distribution
    * Append UIMA_HOME/bin to your PATH
    
    * Please run the script UIMA_HOME/bin/adjustExamplePaths.bat (or .sh), to update 
      paths in the examples based on the actual UIMA_HOME directory path. 
      This script runs a Java program; 
      you must either have java in your PATH or set the environment variable JAVA_HOME to a 
      suitable JRE.

    Note: The Mac OS X operating system procedures for setting up global environment
    variables are described here: see http://developer.apple.com/qa/qa2001/qa1067.html.
      
      
Verifying Your Installation
----------------------------

To test the installation, run the documentAnalyzer.bat (or .sh) file located in the bin subdirectory. 
This should pop up a "Document Analyzer" window. Set the values displayed in this GUI to as follows:

    * Input Directory: UIMA_HOME/examples/data
    * Output Directory: UIMA_HOME/examples/data/processed
    * Location of Analysis Engine XML Descriptor: UIMA_HOME/examples/descriptors/analysis_engine/PersonTitleAnnotator.xml

Replace UIMA_HOME above with the path of your Apache UIMA installation.

Next, click the "Run" button, which should, after a brief pause, pop up an "Analyzed Results" window. 
Double-click on one of the documents to display the analysis results for that document.


Getting Started
----------------

For an introduction to Apache UIMA and how to use it, please read the documentation 
located in the docs subdirectory.  A good place to start is the overview_and_setup 
book's first chapter, which has a brief guide to the documentation.