
      Apache uimaFIT (TM) v2.4.0
      --------------------------


What is uimaFIT?
----------------

uimaFIT is a part of the Apache UIMA(TM) project. uimaFIT can only be used in conjunction with
a compatible version of the Java version of the Apache UIMA SDK. For your convenience, the binary
distribution package of uimaFIT includes all libraries necessary to use uimaFIT. In particular for
novice users, it is strongly advised to obtain a copy of the full UIMA SDK separately.

uimaFIT is a library that provides factories, injection, and testing utilities for UIMA. The
following list highlights some of the features uimaFIT provides:

 * Factories: simplify instantiating UIMA components programmatically without descriptor files.
   For example, to instantiate an AnalysisEngine a call like this could be made:

     AnalysisEngineFactory.createEngine(MyAEImpl.class, myTypeSystem, paramName, paramValue, ...)

 * Injection: handles the binding of configuration parameter values to the corresponding member
   variables in the analysis engines and handles the binding of external resources. For example,
   to bind a configuration parameter just annotate a member variable with @ConfigurationParameter.
   Then add one line of code to your initialize method:

     ConfigurationParameterInitializer.initialize(this, uimaContext).

   This is handled automatically if you extend the uimaFIT JCasAnnotator_ImplBase class.

 * Testing: uimaFIT simplifies testing in a number of ways described in the documentation. By making
   it easy to instantiate your components without descriptor files a large amount of
   difficult-to-maintain and unnecessary XML can be eliminated from your test code. This makes tests
   easier to write and maintain. Also, running components as a pipeline can be accomplished with a
   method call like this:

     SimplePipeline.runPipeline(reader, ae1, ..., aeN, consumer1, ... consumerN)


What's New in 2.4.0
-------------------

uimaFIT 2.4.0 is a minor feature and bugfix release. It should should serve as a drop-in replacement
for previous uimaFIT 2.x versions.

A full list of issues addressed in this release can be found on the Apache issue tracker:

  https://issues.apache.org/jira/secure/ReleaseNote.jspa?projectId=12310570&version=12340576

Supported Platforms
-------------------

uimaFIT requires Java 1.7 or higher, UIMA 2.10.2 or higher, and the Spring Framework 3.2.16 or higher.


Availability
------------

uimaFIT is licensed under the Apache License 2.0 and is available from the Apache UIMA project:

  http://uima.apache.org
  http://svn.apache.org/repos/asf/uima/uimafit

uimaFIT is available via Maven Central. If you use Maven for your build environment, then you can
add uimaFIT as a dependency to your pom.xml file with the following:

  <dependency>
    <groupId>org.apache.uima</groupId>
    <artifactId>uimafit-core</artifactId>
    <version>2.4.0</version>
  </dependency>


Modules
-------

uimafit-core           - the main uimaFIT module
uimafit-cpe            - support for the Collection Processing Engine (multi-threaded pipelines)
uimafit-maven          - a Maven plugin to automatically enhance UIMA components with uimaFIT
                         metadata and to generate XML descriptors for uimaFIT-enabled components.
uimafit-legacy-support - allows uimaFIT 2.x.0 to use uimaFIT 1.4.x meta data like Java annotations
                         and META-INF/org.uimafit/types.txt files. Pipelines mixing uimaFIT 1.4.x
                         and 2.x components MUST be created using the 2.x factories, because the
                         1.4.x factories will NOT understand how to handle uimaFIT 2.x components
                         or auto-configuration.
uimafit-spring         - an experimental module serving as a proof-of-concept for the integration of
                         UIMA with the Spring Framework. It is currently not considered finished and
                         uses invasive reflection in order to patch the UIMA framework such that it
                         passes all components created by UIMA through Spring to provide for the
                         wiring of Spring context dependencies. This module is made available for
                         the adventurous but currently not considered stable, finished, or even a
                         proper part of the package. E.g. it is not included in the binary
                         distribution package.


Reference
---------

If you use uimaFIT to support academic research, then please consider citing the following paper as
appropriate:

@InProceedings{ogren-bethard:2009:SETQA-NLP,
  author    = {Ogren, Philip  and  Bethard, Steven},
  title     = {Building Test Suites for {UIMA} Components},
  booktitle = {Proceedings of the Workshop on Software Engineering, Testing, and Quality Assurance for Natural Language Processing (SETQA-NLP 2009)},
  month     = {June},
  year      = {2009},
  address   = {Boulder, Colorado},
  publisher = {Association for Computational Linguistics},
  pages     = {1--4},
  url       = {http://www.aclweb.org/anthology/W/W09/W09-1501}
}

History
-------

Since end of 2012, uimaFIT is part of the Apache UIMA project.

Apache uimaFIT was formerly known as uimaFIT, which in turn was formerly known as UUTUC.

Before uimaFIT has become an sub-project within the Apache UIMA project, it is was collaborative
effort between the Center for Computational Pharmacology at the University of Colorado Denver, the
Center for Computational Language and Education Research at the University of Colorado at Boulder,
and the Ubiquitous Knowledge Processing (UKP) Lab at the Technische Universität Darmstadt.

The initial uimaFIT development team was:

Philip Ogren, University of Colorado, USA
Richard Eckart de Castilho, Technische Universität Darmstadt, Germany
Steven Bethard, Stanford University, USA

with contributions from Niklas Jakob, Fabio Mancinelli, Chris Roeder, Philipp Wetzler, Shuo Yang,
Torsten Zesch.


Support
-------

Please direct questions to user@uima.apache.org.
