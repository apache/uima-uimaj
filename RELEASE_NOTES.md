<!--
***************************************************************
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
***************************************************************
-->
   
# Apache UIMA (Unstructured Information Management Architecture) v3.3.0 Release Notes

## Contents

[What is UIMA?](#what.is.uima)  
[Major Changes in this Release](#major.changes)  
[How to Get Involved](#get.involved)  
[How to Report Issues](#report.issues)  
[List of JIRA Issues Fixed in this Release](#list.issues)  

## <a id="what.is.uima">What is UIMA?</a>

Unstructured Information Management applications are software systems that analyze large volumes of
unstructured information in order to discover knowledge that is relevant to an end user. UIMA is a
framework and SDK for developing such applications. An example UIM application might ingest plain
text and identify entities, such as persons, places, organizations; or relations, such as works-for
or located-at. UIMA enables such an application to be decomposed into components, for example
"language identification" -> "language specific segmentation" -> "sentence boundary detection" ->
"entity detection (person/place names etc.)". Each component must implement interfaces defined by
the framework and must provide self-describing metadata via XML descriptor files. The framework
manages these components and the data flow between them. Components are written in Java or C++; the
data that flows between components is designed for efficient mapping between these languages. UIMA
additionally provides capabilities to wrap components as network services, and can scale to very
large volumes by replicating processing pipelines over a cluster of networked nodes.

Apache UIMA is an Apache-licensed open source implementation of the UIMA specification (that 
specification is, in turn, being developed concurrently by a technical committee within
[OASIS](http://www.oasis-open.org), a standards organization). We invite and encourage you to
participate in both the implementation and specification efforts.

UIMA is a component framework for analysing unstructured content such as text, audio and video. It
comprises an SDK and tooling for composing and running analytic components written in Java and C++,
with some support for Perl, Python and TCL.

## <a id="major.changes">Notable changes in this release</a>

* [UIMA-6418] Added support for component parameters of type "long" and "double" 
* [UIMA-6358] Added platform-independent methods for setting the datapath in a resource manager
* [UIMA-6374] Added an extensive CAS (de)serialization test suit
* [UIMA-6431] Added support for using lambda functions as CAS processors
* [UIMA-6412] Changed CPMEngine from using a thread group to using an executor service
* [UIMA-6389] Fixed exceptions being swallowed when using Log4J2 through its SLF4J API
* [UIMA-6386] Fixed wrong UIMA session being set on the ConfigurationManager in aggregates
* [UIMA-6390] Fixed NPE when trying to access config names of fresh context
* [UIMA-6378] Fixed build on Java 16
* [UIMA-6393] Fixed circular imports in descriptors breaking the resource manager cache
* [UIMA-6367] Fixed JCas cover annotation created in PEAR context being replaced by index operations
* [UIMA-6388] Fixed CAS.select(null) returning all annotations instead of throwing an exception
* [UIMA-6423] Fixed selecting a non-existing type returning all types instead of throwing an exception
* [UIMA-6421] Fixed range check when injecting a String value into StringArray slot to throw an exception
* [UIMA-6400] Fixed leaking ThreadLocal in UimaContextHolder
* [UIMA-6398] Fixed memory leak in UIMA loggers and loggers using the wrong classloader for i18n messages
* [UIMA-6413] Fixed memory leak in FSClassRegistry
* [UIMA-6377] Fixed spurious multipleReferencesAllowed warning when serializing empty arrays
* [UIMA-6372] Upgraded to JUnit 5
* [UIMA-6373] Format UIMA Core Java SDK codebase

### API changes

#### SelectFS API with null or non-existing types

When providing `null` or as a type or an non-existing type to a `select` call, then an exception is
is thrown. Previously, all annotations were returned instead. To explicitly select any type, use
the new `anyType()` instead of calling `type(null)`.

#### ResourceManager datapath methods

The methods `getDataPath()` and `setDataPath(String)` which were accepting/returning paths using 
platform-specific path separators have been deprepcated. Instead, use the new 
`setDataPathElements(File/String...)` and `getDataPathElements()` methods.

#### JUnit upgrade

The JUnit module has been upgraded from JUnit 4 to JUnit 5 along with the rest of the test code
switching to JUnit 5. If you use the unit test helpers from this module, you also have to upgrade
your tests to JUnit 5.

## <a id="list.issues">Full list of JIRA Issues affecting this Release</a>

Click [issuesFixed/jira-report.hmtl](issuesFixed/jira-report.html) for the list of issues affecting
this release.

Please use the mailing lists ( http://uima.apache.org/mail-lists.html ) for feedback.

## <a id="get.involved">How to Get Involved</a>

The Apache UIMA project really needs and appreciates any contributions, including documentation 
help, source code and feedback. If you are interested in contributing, please visit 
[http://uima.apache.org/get-involved.html](http://uima.apache.org/get-involved.html).

## <a id="report.issues">How to Report Issues</a>

The Apache UIMA project uses JIRA for issue tracking. Please report any issues you find at 
[http://issues.apache.org/jira/browse/uima](http://issues.apache.org/jira/browse/uima).