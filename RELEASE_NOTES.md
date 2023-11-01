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
   
# Apache UIMA (Unstructured Information Management Architecture) v3.5.0 Release Notes

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
* ⭐️ Issue #327: Provide a BOM
* ⭐️ Issue #341: Deploy Eclipse feature definitions to Maven repo
* 🦟 Issue #315: ThreadContextClassLoader ignored by ResourceManager when extension classloader is set
* 🦟 Issue #320: Copy of FsIterator_set_sorted_pear does not retain position
* 🦟 Issue #337: Component description editor may not open in Eclipse
* 🦟 Issue #346: Helper annotation created by subiterator may remain in CAS
* 🦟 Issue #345: Certain select operations deplete FS ID pool
* ⚙️ Issue #319: SelectFSs_impl.close() creates stream just to close it
* ⚙️ Issue #331: Convert remaining documentation to asciidoc
* ⚙️ Issue #335: Remove dependency on JUnit 4 and JUnit Assert
* ⚙️ Issue #350: Clean up and modernize code
* 🩹 Issue #317: Update dependencies
* 🩹 Issue #325: Update dependencies
* 💀 Issue #339: Drop CasAnnotationViewerApplet and CasTreeViewerApplet


**Full Changelog**: https://github.com/apache/uima-uimaj/compare/rel/uimaj-3.4.1...uimaj-3.5.0


## <a id="get.involved">How to Get Involved</a>

The Apache UIMA project really needs and appreciates any contributions, including documentation 
help, source code and feedback. If you are interested in contributing, please visit 
[http://uima.apache.org/get-involved.html](http://uima.apache.org/get-involved.html).

## <a id="report.issues">How to Report Issues</a>

The Apache UIMA project uses GitHub for issue tracking. Please report any issues you find at 
[https://github.com/apache/uima-uimaj/issues](https://github.com/apache/uima-uimaj/issues).
