<!--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Notes for AI coding agents

Short rules for agents working on the Apache UIMA Java SDK (uimaj) and uimaFIT.
Human contributors follow the same rules. The other Apache UIMA repositories
follow them as well, adapting the module and build details to their own layout.

## Layout

- `uimaj-core`: the CAS, type system, JCas, serialization formats and the
  framework APIs. Most changes land here.
- `uimaj-cpe`, `uimaj-tools`, `uimaj-json`, `uimaj-adapter-vinci`, `jVinci`,
  `uimaj-document-annotation`, `uimaj-v3migration-jcas`: further runtime
  modules and tools.
- `uimafit-*`: uimaFIT (`uimafit-core` plus JUnit, AssertJ, Spring, CPE and
  Maven plugin integrations).
- `jcasgen-maven-plugin`, `PearPackagingMavenPlugin`, `uimafit-maven-plugin`,
  `uima-bnd-plugin`: build tooling.
- `uimaj-ep-*`, `aggregate-uimaj-eclipse-plugins`, `uimaj-eclipse-*`: the
  Eclipse plugins, built with Tycho.
- `uimaj-test-util`, `uimaj-component-test-util`: shared test helpers.
- `uimaj-documentation`: the manuals in AsciiDoc (`src/docs/asciidoc`).
- `uimaj-parent`: the parent for external projects building on UIMA.
  `uimaj-parent-internal`: the parent for the modules in this repository.
  `aggregate-uimaj`: the reactor that lists the modules. `uimaj-bom`: the BOM.
  `uimaj-dist`: the binary and source distributions.

## Rules

Agents follow [CONTRIBUTING.md](CONTRIBUTING.md) like human contributors: the
build, code, Javadoc, test, commit and pull request rules, and the policy on
AI tools. The points below are the ones that must never be missed.

- Disclose AI help in the commit message with an `Assisted-by:` trailer
  naming the tool and its version and the model and its version, for example
  `Assisted-by: ExampleCoder 2.1.0 (example-model 4.5)`.
- An agent may edit, build and commit locally. It should not push, open pull
  requests, file issues, post comments or reply to reviewers on its own, only
  with the explicit approval of the person using it.
- Run `mvn verify` on the touched modules before proposing a change. Say which
  commands ran, which failed and which were skipped. Never report a check as
  passed that did not run.
- Do not introduce new dependencies without a discussion and approval.
