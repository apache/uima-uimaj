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
   
# Apache UIMA (Unstructured Information Management Architecture) v3.6.0 Release Notes

## Contents

[What is UIMA?](#what.is.uima)  
[Major Changes in this Release](#major.changes)  
[List of JIRA Issues Fixed in this Release](#list.issues)  
[How to Get Involved](#get.involved)  
[How to Report Issues](#report.issues)  

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
comprises an SDK and tooling for composing and running analytic components written in Java and C++.

## <a id="major.changes">Notable changes in this release</a>

This is a feature and bug fix release.

## System requirements updated

* Minimum Java version required: 17 or later
* Minimum Eclipse version required: 4.25 (2022-09)


## What's Changed
* ⭐️ Issue #372: Allow adding URLs to the datapath
* ⭐️ Issue #348: Varargs for description class setters
* ⭐️ Issue #369: Move isTypeName and isFeatureName to public API
* ⭐️ Issue #402: Provide bnd plugin to generate package imports based on imports in UIMA descriptors
* ⭐️ Issue #382: Warning when PEAR contains a JCAS class that is used as a feature range outside the PEAR
* ⭐️ Issue #385: Allow import of type systems published through SPI
* ⭐️ Issue #387: Simplify creation of new UIMAContext
* ⭐️ Issue #390: Merge uimaFIT modules into UIMA-J repository
* ⭐️ Issue #393: Include uimaFIT artifacts in binary distribution
* 🦟 Issue #368: select(AnnotationBaseFs-type).count() seems to return MAX_LONG
* 🦟 Issue #371: Repeated creation of type systems can exhaust JVM metaspace
* 🦟 Issue #395: Potential failure to look up UIMA-internal classes in OSGI-like contexts
* ⚙️ Issue #379: Clean up code
* ⚙️ Issue #398: Mark Maven plugins as thread-safe
* ⚙️ Issue #404: Remove toolchains
* ⚙️ Issue #407: Clean up PearPackagingMavenPlugin
* ⚙️ Issue #409: Update dependencies
* ⚙️ Issue #417: BOM should not inherit build setup from parent POM


**Full Changelog**: https://github.com/apache/uima-uimaj/compare/rel/uimaj-3.5.1...uimaj-3.6.0


## <a id="get.involved">How to Get Involved</a>

The Apache UIMA project really needs and appreciates any contributions, including documentation 
help, source code and feedback. If you are interested in contributing, please visit 
[http://uima.apache.org/get-involved.html](http://uima.apache.org/get-involved.html).

## <a id="report.issues">How to Report Issues</a>

The Apache UIMA project uses GitHub for issue tracking. Please report any issues you find at 
[https://github.com/apache/uima-uimaj/issues](https://github.com/apache/uima-uimaj/issues).
