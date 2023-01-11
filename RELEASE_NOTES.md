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
   
# Apache UIMA (Unstructured Information Management Architecture) v3.3.1 Release Notes

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
comprises an SDK and tooling for composing and running analytic components written in Java and C++,
with some support for Perl, Python and TCL.

## <a id="major.changes">Notable changes in this release</a>

This is a bug fix release.

**Bugs fixed**
* 🦟 Issue #255: File handle leak accessing performanceTuning.properties
* 🦟 Issue #240: Helper annotation created by SelectFS should not survive
* 🦟 Issue #238: Form 6 serializes non-reachable FSes but should not
* 🦟 Issue #235: Misleading error message when JCas type is not registered
* 🦟 [UIMA-6479] PearPackagingMavenPlugin has ancient JUnit dependency
* 🦟 [UIMA-6473] CasToComparableText is broken

**Improvements**
* ⭐️ Issue #222: Support comparing test files irrespective of line endings
* ⭐️ [UIMA-6480] Add tests with empty arrays to CAS de/ser-suite

**Refactoring**
* ⚙️ [UIMA-6454] Update dependencies
* ⚙️ [UIMA-6463] Use toolchains to ensure compatibility with Java 1.8
* ⚙️ [UIMA-6469] Cleaning up file handling code

For a full list of issues affecting this release, please see:

* [GitHub issues](issuesFixed/github-report.html) [[online](https://github.com/apache/uima-uimaj/issues?q=milestone%3A3.3.1)]
* [Jira issues (legacy)](issuesFixed/jira-report.html) [[online](https://issues.apache.org/jira/issues/?jql=project%20%3D%20UIMA%20AND%20fixVersion%20%3D%203.3.1SDK)]

## <a id="get.involved">How to Get Involved</a>

The Apache UIMA project really needs and appreciates any contributions, including documentation 
help, source code and feedback. If you are interested in contributing, please visit 
[http://uima.apache.org/get-involved.html](http://uima.apache.org/get-involved.html).

## <a id="report.issues">How to Report Issues</a>

The Apache UIMA project uses GitHub for issue tracking. Please report any issues you find at 
[https://github.com/apache/uima-uimaj/issues](https://github.com/apache/uima-uimaj/issues).