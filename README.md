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

Running the Eclipse plugin tooling for UIMA requires you start Eclipse 4.29 (2023-09) or later using a Java 17 or later.

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

[UIMA]: https://uima.apache.org
[OASIS-UIMA]: https://www.oasis-open.org/committees/tc_home.php?wg_abbrev=uima
[MAVEN-CENTRAL]: https://search.maven.org/search?q=org.apache.uima
[DKPRO-CASSIS]: https://github.com/dkpro/dkpro-cassis
