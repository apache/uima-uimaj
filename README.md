[![Maven Central](https://img.shields.io/maven-central/v/org.apache.uima/uimafit-core?style=for-the-badge)](https://search.maven.org/search?q=g:org.apache.uima%20a:uimafit*)

Apache uimaFIT (TM)
===================


What is uimaFIT?
----------------

Configuring UIMA components is generally achieved by creating XML descriptor
files which tell the framework at runtime how components should be
instantiated and deployed. These XML descriptor files are very tightly
coupled with the Java implementation of the components they describe.
We have found that it is very difficult to keep the two consistent
with each other especially when code refactoring is very frequent.
uimaFIT provides Java annotations for describing UIMA components which
can be used to directly describe the UIMA components in the code. This
greatly simplifies refactoring a component definition (e.g. changing a
configuration parameter name). It also makes it possible to generate
XML descriptor files as part of the build cycle rather than being
performed manually in parallel with code creation. uimaFIT also makes
it easy to instantiate UIMA components without using XML descriptor
files at all by providing a number of convenience factory methods
which allow programmatic/dynamic instantiation of UIMA components.
This makes uimaFIT an ideal library for testing UIMA components
because the component can be easily instantiated and invoked without
requiring a descriptor file to be created first. uimaFIT is also
helpful in research environments in which programmatic/dynamic
instantiation of a pipeline can simplify experimentation. For example,
when performing 10-fold cross-validation across a number of
experimental conditions it can be quite laborious to create a
different set of descriptor files for each run or even a script that
generates such descriptor files. uimaFIT is type system agnostic and
does not depend on (or provide) a specific type system.

uimaFIT is a library that provides factories, injection, and testing utilities for UIMA. The following list highlights some of the features uimaFIT provides:

* **Factories:** simplify instantiating UIMA components programmatically 
  without descriptor files. For example, to instantiate an AnalysisEngine a
  call like this could be made:

      AnalysisEngineFactory.createEngine(MyAEImpl.class, myTypeSystem,
        paramName1, paramValue2, 
        paramName2, paramValue2, 
        ...)

* **Injection:** handles the binding of configuration parameter values to the 
  corresponding member variables in the analysis engines and handles the binding of 
  external resources. For example, to bind a configuration parameter just annotate 
  a member variable with `@ConfigurationParameter`. External resources can likewise 
  by injected via the `@ExternalResource` annotation.
  Then add one line of code to your initialize method:

      ConfigurationParameterInitializer.initialize(this, uimaContext).

   This is handled automatically if you extend the uimaFIT `JCasAnnotator_ImplBase` class. 

* **Testing:** uimaFIT simplifies testing in a number of ways described in the 
   documentation. By making it easy to instantiate your components without 
   descriptor files a large amount of difficult-to-maintain and unnecessary XML can 
   be eliminated from your test code. This makes tests easier to write and 
   maintain. Also, running components as a pipeline can be accomplished with a
   method call like this:

      SimplePipeline.runPipeline(reader, ae1, ..., aeN, consumer1, ... consumerN)

uimaFIT is a part of the Apache UIMA(TM) project. uimaFIT can only be used in 
conjunction with a compatible version of the Java version of the Apache UIMA SDK. 
For your convenience, the binary distribution package of uimaFIT includes all 
libraries necessary to use uimaFIT. In particular for novice users, it is strongly 
advised to obtain a copy of the full UIMA SDK separately.


Availability
------------

uimaFIT is licensed under the Apache License 2.0 and is available from the Apache UIMA project:

  https://uima.apache.org
  https://github.com/apache/uima-uimafit

uimaFIT is available via Maven Central. If you use Maven for your build 
environment, then you can add uimaFIT as a dependency to your pom.xml file with the 
following:

    <dependency>
      <groupId>org.apache.uima</groupId>
      <artifactId>uimafit-core</artifactId>
      <version>3.2.0</version>
    </dependency>


Modules
-------

- **uimafit-core** - the main uimaFIT module
- **uimafit-cpe** - support for the Collection Processing Engine 
  (multi-threaded pipelines)
- **uimafit-maven** - a Maven plugin to automatically enhance UIMA components with 
  uimaFIT metadata and to generate XML descriptors for uimaFIT-enabled components.
- **uimafit-junit** - convenience code facilitating the implementation of UIMA/
  uimaFIT tests in JUnit tests
- **uimafit-assertj** - adds assertions for UIMA/uimaFIT types via the AssertJ 
  framework
- **uimafit-spring** - an experimental module serving as a proof-of-concept for the 
  integration of UIMA with the Spring Framework. It is currently not considered 
  finished and uses invasive reflection in order to patch the UIMA framework such 
  that it passes all components created by UIMA through Spring to provide for the
  wiring of Spring context dependencies. This module is made available for
  the adventurous but currently not considered stable, finished, or even a
  proper part of the package. E.g. it is not included in the binary
  distribution package.


Reference
---------

If you use uimaFIT to support academic research, then please consider citing the 
following paper as appropriate:

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
