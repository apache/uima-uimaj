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
   
# Apache UIMA (Unstructured Information Management Architecture) v3.4.0 Release Notes

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

This is a feature fix release.


## What's Changed

**Improvements**
* ⭐️ [UIMA-6474] Switch to getDataPathElements() in UIMA context by @reckart in https://github.com/apache/uima-uimaj/pull/216
* ⭐️ Issue #226: Provide SPI interfaces to locate descriptors by @reckart in https://github.com/apache/uima-uimaj/pull/227, https://github.com/apache/uima-uimaj/pull/237, https://github.com/apache/uima-uimaj/pull/249
* ⭐️ Issue #225: Improve deployment in OSGi environments by @reckart in https://github.com/apache/uima-uimaj/pull/232, https://github.com/apache/uima-uimaj/pull/244, https://github.com/apache/uima-uimaj/pull/250
* ⭐️ Issue #245: Utility method for loading type systems in SPI providers by @reckart in https://github.com/apache/uima-uimaj/pull/246
* ⭐️ Issue #247: RelativePathResolver should consider TCCL by @reckart in https://github.com/apache/uima-uimaj/pull/248
* ⭐️ Issue #268: UIMA components log and then re-throw exceptions which usually leads to errors being logged twice by @reckart in https://github.com/apache/uima-uimaj/pull/279

**Bugs fixed**
* 🦟 Issue #252: Potential failure to look up FsGenerator3 in OSGI-like contexts by @reckart in https://github.com/apache/uima-uimaj/pull/253
* 🦟 Issue #265: Unable to release without auto-staging by @reckart in https://github.com/apache/uima-uimaj/pull/282
* 🦟 Issue #266: Unable to install UIMA 3.3.1 Eclipse Plugins in Eclipse 2022-09 by @reckart in https://github.com/apache/uima-uimaj/pull/274
* 🦟 Issue #267: UIMA Log4jLogger_impl not compatible with log4j 2.18.0+ by @reckart in https://github.com/apache/uima-uimaj/pull/269, https://github.com/apache/uima-uimaj/pull/280
* 🦟 Issue #272: select on FSArray seems broken by @reckart in https://github.com/apache/uima-uimaj/pull/277, https://github.com/apache/uima-uimaj/pull/278
* 🦟 Issue #275: Improved error message made it even more misleading by @reckart in https://github.com/apache/uima-uimaj/pull/276
* 🦟 Issue #285: NPE while deserializing an XMI in a PEAR context by @reckart in https://github.com/apache/uima-uimaj/pull/287

**Refactorings**
* ⚙️ [UIMA-6440] Stage release artifacts as part of the build by @reckart in https://github.com/apache/uima-uimaj/pull/199
* ⚙️ [UIMA-6443] Fresh Eclipse update site for every release by @reckart in https://github.com/apache/uima-uimaj/pull/200
* ⚙️ [UIMA-6462] Avoid deploy broken checksum files for p2content.xml and artifacts.xml in feature modules by @reckart in https://github.com/apache/uima-uimaj/pull/205
* ⚙️ [UIMA-6463] Use toolchains to ensure compatibility with Java 1.8 by @reckart in https://github.com/apache/uima-uimaj/pull/206
* ⚙️ [UIMA-6436] Move maintainer documentation from website into maintainer guide by @reckart in https://github.com/apache/uima-uimaj/pull/197, https://github.com/apache/uima-uimaj/pull/217
* ⚙️ Issue #230: Remove version overrides in Maven plugin modules by @reckart in https://github.com/apache/uima-uimaj/pull/231
* ⚙️ Issue #228: Move the UimaDecompiler class by @reckart in https://github.com/apache/uima-uimaj/pull/229
* ⚙️ Issue #283: Update issue report generation by @reckart in https://github.com/apache/uima-uimaj/pull/284
* 🩹 [UIMA-6459] Upgrade dependencies by @reckart in https://github.com/apache/uima-uimaj/pull/204, https://github.com/apache/uima-uimaj/pull/207
* 🩹 Issue #270: Update dependencies by @reckart in https://github.com/apache/uima-uimaj/pull/271, https://github.com/apache/uima-uimaj/pull/273


For a full list of issues affecting this release, please see:

* [GitHub issues](issuesFixed/github-report.html) [[online](https://github.com/apache/uima-uimaj/issues?q=milestone%3A3.4.0)]
* [Jira issues (legacy)](issuesFixed/jira-report.html) [[online](https://issues.apache.org/jira/issues/?jql=project%20%3D%20UIMA%20AND%20fixVersion%20%3D%203.4.0SDK)]

## <a id="get.involved">How to Get Involved</a>

The Apache UIMA project really needs and appreciates any contributions, including documentation 
help, source code and feedback. If you are interested in contributing, please visit 
[http://uima.apache.org/get-involved.html](http://uima.apache.org/get-involved.html).

## <a id="report.issues">How to Report Issues</a>

The Apache UIMA project uses GitHub for issue tracking. Please report any issues you find at 
[https://github.com/apache/uima-uimaj/issues](https://github.com/apache/uima-uimaj/issues).
