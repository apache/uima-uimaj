Welcome to the Apache UIMA Java SDK
-----------------------------------

[Apache UIMA][UIMA] helps you managing unstructured data (such as texts) that is enriched useful
information. For example, if you want to identify a mention of an entity in a text or possible
link that entity to a reference dataset, then Apache UIMA provides:

* a convenient data structure --the Common Analysis Structure (CAS)-- to represent that data
* a type system concept service as a schema for the enriched data that is stored in the CAS
* a component model consisting of reader, analysis engines (processors) and consumers (writers) to
  process that data
* a model for aggregating multiple analysis engines into pipelines and executing them (optionally 
  parallelized)
* various options for (de)serializing the CAS from/to different formats
* any many additional features!

Note the Apache UIMA Java SDK only provides a framework for building analytics but it does not 
provide any analytics. However, there are various [third-parties](#uima-component-providers) that
build on Apache UIMA and that provide collections of analysis components or ready-made solutions.

#### System requirements

Apache UIMA v3.5.0 and later requires Java version 17 or later.

Running the Eclipse plugin tooling for UIMA requires you start Eclipse 4.25 (2022-09) or later using a Java 17 or later.

Running the migration tool on class files requires running with a Java JDK, not a Java JRE.

The supported platforms are: Windows, Linux, and macOS. Other Java platform implementations should
work but have not been significantly tested.

Many of the scripts in the `/bin` directory invoke Java. They use the value of the environment 
variable, `JAVA_HOME`, to locate the Java to use; if it is not set, they invoke `java` expecting to find
an appropriate Java in your `PATH` variable. 


#### Using Apache UIMA Java SDK

You can add the Apache UIMA Java SDK to your project easily in most build tools by importing it from 
[Maven Central][MAVEN-CENTRAL]. For example if you use Maven, you can add the following dependency
to your project:

```xml
<dependency>
  <groupId>org.apache.uima</groupId>
  <artifactId>uimaj-core</artifactId>
  <version>3.5.0</version>
</dependency>
```

Next, we give a few brief examples of how to use the Apache UIMA Java SDK and the Apache uimaFIT library.
Apache uimaFIT is a separate dependency that you can add:

```xml
<dependency>
  <groupId>org.apache.uima</groupId>
  <artifactId>uimafit-core</artifactId>
  <version>3.5.0</version>
</dependency>
```

##### Creating a type system

The type system defines the type of information that we want to attach to the unstructured information (here a text document). In our example, we want to identify mentions of entities, so we define a type my.Entity with a feature category which can be used to store the category the entity belongs to.

To illustrate the information UIMA internally maintains about the annotation schema, we write the generated schema as XML to screen.

```java
String TYPE_NAME_ENTITY = "my.Entity";
String TYPE_NAME_TOKEN = "my.Token";
String FEAT_NAME_CATEGORY = "category";

var tsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();
tsd.addType(TYPE_NAME_TOKEN, "", CAS.TYPE_NAME_ANNOTATION);
var entityTypeDesc = tsd.addType(TYPE_NAME_ENTITY, "", CAS.TYPE_NAME_ANNOTATION);
entityTypeDesc.addFeature(FEAT_NAME_CATEGORY, "", CAS.TYPE_NAME_STRING);

tsd.toXML(System.out);
```
##### Creating a Common Analaysis Structure object

Now we create a Common Analysis Structure (CAS) object into which we store the text that we want to analyse.

Again, to illustrate the information that UIMA internally stores in the CAS object, we write an XML representation of the object to screen.

```java
var cas = CasFactory.createCas(tsd);
cas.setDocumentText("Welcome to Apache UIMA.");
cas.setDocumentLanguage("en");

CasIOUtils.save(cas, System.out, SerialFormat.XMI_PRETTY);
```

##### Adding and retrieving annotations

Now, we create an annotation of the type `my.Entity` to identify the mention of `Apache UIMA` in the example text.

Finally, we iterate over all annotations in the CAS and print them to screen. This includes the default `DocumentAnnotation` that is always created by UIMA as
well as the `my.Entity` annotation that we created ourselves.

```java
var entityType = cas.getTypeSystem().getType(TYPE_NAME_ENTITY);
var entity = cas.createAnnotation(entityType, 11, 22);
cas.addFsToIndexes(entity);

for (var anno : cas.<Annotation>select(entityType)) {
   System.out.printf("%s: [%s]%n", anno.getType().getName(), anno.getCoveredText());
}
```

##### Working with analysis components

In order to organize different types of analysis into steps, we usually package them into individual analysis engines. We illustrate now how such components can be built and how they can be put executed as an analysis pipeline.

```java
class TokenAnnotator extends CasAnnotator_ImplBase {
  public void process(CAS cas) throws AnalysisEngineProcessException {
    var tokenType = cas.getTypeSystem().getType(TYPE_NAME_TOKEN);
    var bi = BreakIterator.getWordInstance();
    bi.setText(cas.getDocumentText());
    int begin = bi.first();
    int end;
    for (end = bi.next(); end != BreakIterator.DONE; end = bi.next()) {
      var token = cas.createAnnotation(tokenType, begin, end);
      cas.addFsToIndexes(token);
      begin = end;
    }
  }
}

class EntityAnnotator extends CasAnnotator_ImplBase {
  public void process(CAS cas) throws AnalysisEngineProcessException {
    var tokenType = cas.getTypeSystem().getType(TYPE_NAME_TOKEN);
    var entityType = cas.getTypeSystem().getType(TYPE_NAME_ENTITY);
    for (var token : cas.<Annotation>select(tokenType)) {
      if (Character.isUpperCase(token.getCoveredText().charAt(0))) {
        var entity = cas.createAnnotation(entityType, token.getBegin(), token.getEnd());
        cas.addFsToIndexes(entity);
      }
    }
  }
}

cas = CasFactory.createCas(tsd);
cas.setDocumentText("John likes Apache UIMA.");
cas.setDocumentLanguage("en");

var pipeline = AnalysisEngineFactory.createEngineDescription(
  AnalysisEngineFactory.createEngineDescription(TokenAnnotator.class),
  AnalysisEngineFactory.createEngineDescription(EntityAnnotator.class));

SimplePipeline.runPipeline(cas, pipeline);

for (var anno : cas.<Annotation>select(entityType)) {
   System.out.printf("%s: [%s]%n", anno.getType().getName(), anno.getCoveredText());
}
```


#### Using uimaFIT

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

uimaFIT is a library that provides factories, injection, and testing 
utilities for UIMA. The following list highlights some of the features 
uimaFIT provides:

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

uimaFIT is available via Maven Central. If you use Maven for your build 
environment, then you can add uimaFIT as a dependency to your pom.xml file with the 
following:

    <dependencies>
      <dependency>
        <groupId>org.apache.uima</groupId>
        <artifactId>uimafit-core</artifactId>
        <version>3.5.0</version>
      </dependency>
    </dependencies>
    

**Modules**
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


#### Building

To build Apache UIMA, you need at least a Java 17 JDK and a recent Maven 3 version.

After extracting the source distribution ZIP or cloning the repository, change into the created
directory and run the following command:

```
mvn clean install
```

For more details, please see http://uima.apache.org/building-uima.html


#### Running examples from the source/binary distribution

You can download the source and binary distributions from the
[Apache UIMA website](https://uima.apache.org/downloads.cgi).

##### Environment Variables

After you have unpacked the Apache UIMA distribution from the package of your choice (e.g. `.zip` or 
`.gz`), perform the steps below to set up UIMA so that it will function properly.

* Set `JAVA_HOME` to the directory of your JRE installation you would like to use for UIMA.  
* Set `UIMA_HOME` to the `apache-uima` directory of your unpacked Apache UIMA distribution
* Append `UIMA_HOME/bin` to your `PATH`
* Please run the script `UIMA_HOME/bin/adjustExamplePaths.bat` (or `.sh`), to update 
  paths in the examples based on the actual `UIMA_HOME` directory path. 
  This script runs a Java program; you must either have `java` in your `PATH` or set the environment 
  variable `JAVA_HOME` to a suitable JRE.

    Note: The Mac OS X operating system procedures for setting up global environment
    variables are described here: see http://developer.apple.com/qa/qa2001/qa1067.html.
      
##### Verifying Your Installation

To test the installation, run the `documentAnalyzer.bat` (or `.sh`) file located in the `bin` subdirectory. 
This should pop up a *Document Analyzer* window. Set the values displayed in this GUI to as follows:

* Input Directory: `UIMA_HOME/examples/data`
* Output Directory: `UIMA_HOME/examples/data/processed`
* Location of Analysis Engine XML Descriptor: `UIMA_HOME/examples/descriptors/analysis_engine/PersonTitleAnnotator.xml`

Replace `UIMA_HOME` above with the path of your Apache UIMA installation.

Next, click the *Run* button, which should, after a brief pause, pop up an *Analyzed Results* window. 
Double-click on one of the documents to display the analysis results for that document.


#### UIMA component providers

Here is list of several well-known projects that provide their analysis tools as UIMA components
or that wrap third-party analysis tools as UIMA components:

* [Apache cTAKES](https://ctakes.apache.org) - Natural language processing system for extraction of information from electronic medical record clinical free-text.
* [Apache OpenNLP](https://opennlp.apache.org/docs/) - Wraps OpenNLP for UIMA. Adaptable to different type systems.
* [Apache Ruta](https://uima.apache.org/ruta.html) - Generic rule-based text analytics. Works with any type system.
* [ClearTK](https://cleartk.github.io/cleartk/) - Wraps several third-party tools (OpenNLP, CoreNLP, etc.) and offers a flexible framework for training own machine learning models. Uses CleartK type system.
* [DKPro Core](https://dkpro.github.io/dkpro-core/) - Wraps many third-party tools (OpenNLP, CoreNLP, etc.) and supporting a wide range of data formats. Uses DKPro Core type system.
* [JULIE Lab Component Repository (JCoRe)](https://github.com/JULIELab/jcore-base) Wraps several third-party tools (OpenNLP, CoreNLP, etc.) and supporting a wide range of data formats, in particular from the biomed domain. Uses JCore type system.

This is not an exhaustive list. If you feel any particular project should be listed here, please let us know.
You could find additional ones e.g. by:

* following the [GitHub dependency graph](https://github.com/apache/uima-uimaj/network/dependents?package_id=UGFja2FnZS0xNzk4MzkxNTI%3D)
* searching [Google Scholar for UIMA](https://scholar.google.com/scholar?hl=en&q=uima)

#### Interoperability

The Apache UIMA Java SDK can be used with any programming language based on the Java Virtual Machine
including Java, Groovy, Scala, and many other languages.

Interoperability with Python can for example be achieved via the third-party 
[DKPro Cassis][DKPRO-CASSIS] library which can be used to read, manipulate and write CAS data in the
XMI format.

#### Further reading

The Apache UIMA Java SDK is a Java-based implementation of the [UIMA specification][OASIS-UIMA].

#### Support

Please direct questions to user@uima.apache.org.

#### Reference

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

#### History

* **Early 2000s:** UIMA was originally developed by IBM as part of research into analyzing unstructured information (like text, audio, and video). It was designed to process large volumes of unstructured data in a scalable way, targeting natural language processing (NLP) applications.

* **2004:** UIMA was open-sourced allowing for broader use and contributions from outside IBM.

* **2006:** The UIMA project was accepted into the Apache Incubator, starting the formal process of becoming an Apache project.

* **2008:** UIMA graduated from the Apache Incubator and became a top-level Apache project, signifying its maturity and active development.

* **2009:** Apache UIMA-AS (Asynchronous Scaleout) was introduced, enabling distributed and asynchronous processing of UIMA pipelines.

* **2012:** uimaFIT was contributed to the Apache UIMA project. Apache uimaFIT was formerly known as uimaFIT, which in turn was formerly known as UUTUC. Prior to its contribution, is was collaborative
effort between the Center for Computational Pharmacology at the University of Colorado Denver, the
Center for Computational Language and Education Research at the University of Colorado at Boulder,
and the Ubiquitous Knowledge Processing (UKP) Lab at the Technische Universität Darmstadt.

* **2013:** UIMA DUCC (Distributed UIMA Cluster Computing) was introduced as a sub-project of Apache UIMA.

* **2016:** Apache UIMA Ruta (Rule-based Text Annotation) was introduced as an extension, providing a scripting language for rule-based text processing.

* **2023:** UIMA DUCC and UIMA-AS were retired.

* **2024:** uimaFIT has been merged into the UIMA Java SDK

[UIMA]: https://uima.apache.org
[OASIS-UIMA]: https://www.oasis-open.org/committees/tc_home.php?wg_abbrev=uima
[MAVEN-CENTRAL]: https://search.maven.org/search?q=org.apache.uima
[DKPRO-CASSIS]: https://github.com/dkpro/dkpro-cassis

