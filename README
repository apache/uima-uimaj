
			Apache UIMA (Unstructured Information Management Architecture) v2.4.0 SDK
		  -------------------------------------------------------------------------

Building from the Source Distribution
-------------------------------------

We use Maven 3.0 for building; download this if needed, 
and set the environment variable MAVEN_OPTS to -Xmx800m -XX:MaxPerSize-256m.

Then do the build by going into the .../uimaj directory, and issuing the command
   mvn clean install
   
This builds everything except the ...source-release.zip file. If you want that,
change the command to 

   mvn clean install -Papache-release
   
Look for the result here: 
   target/uimaj-[version]-source-release.zip (if run with -Papache-release)

For more details, please see http://uima.apache.org/building-uima.html   

What's New in 2.4.0
-------------------

  There was a change in the API (methods added) used for JMX monitoring of performance statistics;
  because of this, we incremented the version from 2.3.x to 2.4.0.
  
  Other than this, there are some bug fixes, and some tooling enhancements.
  
  CAS Editor (Eclipse Tooling)
  ----------------------------
  The Cas Editor received a few important enhancements to make it
  extensible by user provided plugins. It is now possible to save
  generic preferences based on a type system scope and the Cas Editor
  was split into two parts to allow the integration into RCP applications.

  Support for Cas Editor projects is now removed, existing Cas Editor
  projects will be migrated automatically. Before this happens a dialog
  ask for confirmation.
  
  There have been a couple of minor improvements: Adding of annotations
  to a CAS is now much faster, reusing of Annotation Editor instances is
  now possible, CAS file changes can now be detected and the changed file
  is shown in the editor, many things are now remembered after a dialog
  is reopened or an editor is reopened.
  
  Eclipse Analysis Engine Launcher Plugin
  ---------------------------------------
  We added a new launcher plugin to run and debug Analysis Engines directly
  from Eclipse. The plugin can load input CASes in the XCAS or XMI format
  from a specified folder and then processes them with the Analysis Engine.
  The files can optionally be written to an output folder for inspection
  with the Cas Editor. Plain text input files are supported as well.
  
  A command line Pear Installer tool was added.

  
  Build
  -----
  
  The build process was redone to align it with normal Maven build procedures, where possible.
  This includes moving the top level project, uimaj, up one level in SVN, so it now contains the
  modules. 
  
 
Supported Platforms
--------------------

Apache UIMA requires Java level 1.5; it has been tested with Sun Java SDK v5.0 and v6.0, and IBM Java 6.0.
Running the Eclipse plugin tooling for UIMA requires you start Eclipse using a Java 1.5 or later, as well.
The supported platforms are: Windows, Linux, Solaris, AIX and Mac OS X.  
Other platforms and Java (1.5+) implementations should work, but have not been significantly tested.

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